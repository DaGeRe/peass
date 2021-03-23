package de.peass.ci;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.config.ExecutionConfig;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.Version;
import de.peass.dependency.reader.DependencyReader;
import de.peass.dependency.reader.VersionKeeper;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.Constants;
import de.peass.vcs.GitCommit;
import de.peass.vcs.VersionIterator;
import de.peass.vcs.VersionIteratorGit;

public class ContinuousDependencyReader {

   private static final Logger LOG = LogManager.getLogger(ContinuousDependencyReader.class);

   private final ExecutionConfig executionConfig;
   private final PeASSFolders folders;
   private final File dependencyFile;
   private final EnvironmentVariables env;

   public ContinuousDependencyReader(final ExecutionConfig executionConfig, final PeASSFolders folders, final File dependencyFile, final EnvironmentVariables env) {
      this.executionConfig = executionConfig;
      this.folders = folders;
      this.dependencyFile = dependencyFile;
      this.env = env;
   }

   Dependencies getDependencies(final VersionIterator iterator, final String url)
         throws Exception {
      Dependencies dependencies;

      final VersionKeeper noChanges = new VersionKeeper(new File(dependencyFile.getParentFile(), "nonChanges_" + folders.getProjectFolder().getName() + ".json"));

      if (!dependencyFile.exists()) {
         dependencies = fullyLoadDependencies(url, iterator, noChanges);
      } else {
         dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
         VersionComparator.setDependencies(dependencies);

         if (dependencies.getVersions().size() > 0) {
            partiallyLoadDependencies(dependencies);
         } else {
            dependencies = fullyLoadDependencies(url, iterator, noChanges);
         }
      }
      VersionComparator.setDependencies(dependencies);

      return dependencies;
   }

   public VersionIterator getIterator(final String lastVersionName) {
      GitCommit lastAnalyzedCommit = new GitCommit(executionConfig.getVersionOld(), "", "", "");
      GitCommit currentCommit = new GitCommit(executionConfig.getVersion(), "", "", "");

      List<GitCommit> commits = new LinkedList<>();
      commits.add(lastAnalyzedCommit);
      commits.add(currentCommit);
      LOG.info("Analyzing {} - {}", lastAnalyzedCommit, currentCommit);
      VersionIteratorGit newIterator = new VersionIteratorGit(folders.getProjectFolder(), commits, lastAnalyzedCommit);
      return newIterator;
   }

   private void partiallyLoadDependencies(final Dependencies dependencies) throws FileNotFoundException, Exception {
      final String lastVersionName = dependencies.getNewestVersion();
      final Version versionDependency = dependencies.getVersions().get(lastVersionName);
      final String predecessingVersionName = versionDependency.getPredecessor();
      
      if (!lastVersionName.equals(executionConfig.getVersion()) || !predecessingVersionName.equals(executionConfig.getVersionOld())) {
         executePartialRTS(dependencies, lastVersionName);
      }
   }

   private void executePartialRTS(final Dependencies dependencies, final String lastVersionName) throws FileNotFoundException {
      File logFile = new File(getDependencyreadingFolder(), executionConfig.getVersion() + "_" + executionConfig.getVersionOld() + ".txt");
      LOG.info("Executing regression test selection update (step 1) - Log goes to ", logFile.getAbsolutePath());
      
      try (LogRedirector director = new LogRedirector(logFile)) {
         VersionIterator newIterator = getIterator(lastVersionName);
         DependencyReader reader = new DependencyReader(folders.getProjectFolder(), dependencyFile, dependencies.getUrl(), newIterator, executionConfig, env);
         newIterator.goTo0thCommit();

         reader.readCompletedVersions(dependencies);
         reader.readDependencies();
      }
   }
   
   public File getDependencyreadingFolder() {
      File folder = new File(dependencyFile.getParentFile(), "dependencyreading");
      if (!folder.exists()) {
         folder.mkdirs();
      }
      return folder;
   }

   private Dependencies fullyLoadDependencies(final String url, final VersionIterator iterator, final VersionKeeper nonChanges)
         throws Exception {
      File logFile = new File(getDependencyreadingFolder(), iterator.getTag() + "_" + iterator.getPredecessor() + ".txt");
      LOG.info("Executing regression test selection (step 1) - Log goes to {}", logFile.getAbsolutePath());

      try (LogRedirector director = new LogRedirector(logFile)) {
         final DependencyReader reader = new DependencyReader(folders, dependencyFile, url, iterator, nonChanges, executionConfig, env);
         iterator.goToPreviousCommit();
         if (!reader.readInitialVersion()) {
            LOG.error("Analyzing first version was not possible");
         } else {
            reader.readDependencies();
         }
         Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
         return dependencies;
      }
   }
}
