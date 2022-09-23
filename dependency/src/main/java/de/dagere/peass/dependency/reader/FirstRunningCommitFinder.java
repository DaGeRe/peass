package de.dagere.peass.dependency.reader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.dependency.RTSTestTransformerBuilder;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.TestTransformer;
import de.dagere.peass.vcs.CommitIterator;

public class FirstRunningCommitFinder {
   
   private static final Logger LOG = LogManager.getLogger(FirstRunningCommitFinder.class);

   private final PeassFolders folders;
   private final VersionKeeper nonRunning;
   private final CommitIterator iterator;
   private final ExecutionConfig executionConfig;
   private final EnvironmentVariables env;

   public FirstRunningCommitFinder(final PeassFolders folders, final VersionKeeper nonRunning, final CommitIterator iterator, final ExecutionConfig executionConfig, final EnvironmentVariables env) {
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
      TestTransformer transformer = RTSTestTransformerBuilder.createTestTransformer(folders, executionConfig, new KiekerConfig(false));
      while (!isVersionRunning && iterator.hasNextCommit()) {
         if (ExecutorCreator.hasBuildfile(folders, transformer)) {
            isVersionRunning = tryCommit(iterator, transformer);
         } else {
            nonRunning.addVersion(iterator.getTag(), "Buildfile does not exist.");
            iterator.goToNextCommit();
         }
      }
      return isVersionRunning;
   }

   private boolean tryCommit(final CommitIterator iterator, final TestTransformer testTransformer) {
      boolean isCommitRunning;
      TestExecutor executor = ExecutorCreator.createExecutor(folders, testTransformer, env);
      isCommitRunning = executor.isCommitRunning(iterator.getTag());

      if (!isCommitRunning) {
         LOG.debug("Buildfile does not exist / commit is not running {}", iterator.getTag());
         if (executor.doesBuildfileExist()) {
            nonRunning.addVersion(iterator.getTag(), "Commit is not running.");
         } else {
            nonRunning.addVersion(iterator.getTag(), "Buildfile does not exist.");
         }
         iterator.goToNextCommit();
      }
      return isCommitRunning;
   }

   private void goToCommit(final CommitIterator iterator) {
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
