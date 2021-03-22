package de.peass.dependency.reader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.config.ExecutionConfig;
import de.peass.dependency.ExecutorCreator;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependency.execution.TestExecutor;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.vcs.VersionIterator;

public class FirstRunningVersionFinder {
   
   private static final Logger LOG = LogManager.getLogger(FirstRunningVersionFinder.class);

   private final PeASSFolders folders;
   private final VersionKeeper nonRunning;
   private final VersionIterator iterator;
   private final ExecutionConfig executionConfig;
   private final EnvironmentVariables env;

   public FirstRunningVersionFinder(final PeASSFolders folders, final VersionKeeper nonRunning, final VersionIterator iterator, final ExecutionConfig executionConfig, final EnvironmentVariables env) {
      this.folders = folders;
      this.nonRunning = nonRunning;
      this.iterator = iterator;
      this.executionConfig = executionConfig;
      this.env = env;
   }

   /**
    * Searches the first commit where a mvn clean packages runs correct, i.e. returns 1
    * 
    * @param projectFolder
    */
   public boolean searchFirstRunningCommit() {
      goToCommit(iterator);
      boolean isVersionRunning = false;
      // The local test transformer enables testing whether a version runs without full configuration
      final JUnitTestTransformer testTransformer = new JUnitTestTransformer(folders.getProjectFolder(), executionConfig);
      while (!isVersionRunning && iterator.hasNextCommit()) {
         if (ExecutorCreator.hasBuildfile(folders)) {
            isVersionRunning = tryCommit(iterator, testTransformer);
         } else {
            nonRunning.addVersion(iterator.getTag(), "Buildfile does not exist.");
            iterator.goToNextCommit();
         }
      }
      return isVersionRunning;
   }

   private boolean tryCommit(final VersionIterator iterator, final JUnitTestTransformer testTransformer) {
      boolean isVersionRunning;
      TestExecutor executor = ExecutorCreator.createExecutor(folders, testTransformer, env);
      isVersionRunning = executor.isVersionRunning(iterator.getTag());

      if (!isVersionRunning) {
         LOG.debug("Buildfile does not exist / version is not running {}", iterator.getTag());
         if (executor.doesBuildfileExist()) {
            nonRunning.addVersion(iterator.getTag(), "Version is not running.");
         } else {
            nonRunning.addVersion(iterator.getTag(), "Buildfile does not exist.");
         }
         iterator.goToNextCommit();
      }
      return isVersionRunning;
   }

   private void goToCommit(final VersionIterator iterator) {
      boolean successGettingCommit = iterator.goToFirstCommit();
      while (!successGettingCommit && iterator.hasNextCommit()) {
         successGettingCommit = iterator.goToNextCommit();
      }
      if (!successGettingCommit) {
         throw new RuntimeException("Repository does not contain usable commit - maybe path has changed?");
      } else {
         LOG.info("Found first commit: " + iterator.getTag());
      }
   }
}
