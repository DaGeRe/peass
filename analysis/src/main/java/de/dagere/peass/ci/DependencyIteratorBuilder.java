package de.dagere.peass.ci;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.vcs.GitCommit;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionIteratorGit;

/**
 * Identifies the versions that should be analyzed, by: (1) Using the versions from ExecutionConfig, if both are given and != null (resolving HEAD and HEAD~1 to the real versions)
 * (2) Using the version from ExecutionConfig, if version is given and versionOld is null, and using the *newest runnable version* from Dependencies
 * 
 * In case (2), the iterator will only be set if analyzing is necessary, i.e. if the *newest version* and version differ. Otherwise, it will be null.
 *
 */
public class DependencyIteratorBuilder {

   private static final Logger LOG = LogManager.getLogger(DependencyIteratorBuilder.class);

   private final String version, versionOld;
   private final VersionIteratorGit iterator;

   public DependencyIteratorBuilder(final ExecutionConfig executionConfig, final StaticTestSelection dependencies, final PeassFolders folders) {
      version = GitUtils.getName(executionConfig.getVersion() != null ? executionConfig.getVersion() : "HEAD", folders.getProjectFolder());

      String newestAnalyzedVersionName = dependencies != null ? dependencies.getNewestVersion() : null;

      GitCommit oldVersionCommit = getOldVersionCommit(executionConfig, newestAnalyzedVersionName, folders);

      if (version.equals(newestAnalyzedVersionName)) {
         LOG.info("Version {} is equal to newest version, not executing RTS", version);
         iterator = null;
         versionOld = getPrePredecessor(dependencies);
      } else if (oldVersionCommit.getTag().equals(version)) {
         LOG.error("Version {} is equal to predecessing version {}, some error occured - not executing RTS", version, oldVersionCommit.getTag());
         iterator = null;
         versionOld = dependencies.getNewestRunningVersion();
      } else {
         if (dependencies != null && 
               dependencies.getVersions().get(newestAnalyzedVersionName) != null && 
               !dependencies.getVersions().get(newestAnalyzedVersionName).isRunning()) {
            versionOld = newestAnalyzedVersionName;
            iterator = null;
         } else {
            GitCommit currentCommit = new GitCommit(version, "", "", "");
            List<GitCommit> commits = new LinkedList<>();
            commits.add(oldVersionCommit);
            commits.add(currentCommit);
            LOG.info("Analyzing {} - {}", oldVersionCommit, currentCommit);
            iterator = new VersionIteratorGit(folders.getProjectFolder(), commits, oldVersionCommit);
            versionOld = oldVersionCommit.getTag();
         }
      }
   }

   private String getPrePredecessor(final StaticTestSelection dependencies) {
      String[] versionNames = dependencies.getVersionNames();
      if (versionNames.length > 1) {
         String prePredecessor = versionNames[versionNames.length - 2];
         return prePredecessor;
      } else {
         return null;
      }
   }

   private static GitCommit getOldVersionCommit(final ExecutionConfig executionConfig, final String newestRunningVersionName, final PeassFolders folders) {
      String oldVersion;
      if (executionConfig.getVersionOld() != null) {
         oldVersion = executionConfig.getVersionOld();
      } else if (newestRunningVersionName != null) {
         oldVersion = newestRunningVersionName;
      } else {
         oldVersion = GitUtils.getName("HEAD~1", folders.getProjectFolder());
      }
      GitCommit oldVersionCommit = new GitCommit(oldVersion, "", "", "");
      return oldVersionCommit;
   }

   public String getVersion() {
      return version;
   }

   public String getVersionOld() {
      return versionOld;
   }

   public VersionIteratorGit getIterator() {
      return iterator;
   }

}
