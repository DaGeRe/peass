package de.dagere.peass.ci;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.FixedCommitConfig;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.vcs.CommitIteratorGit;
import de.dagere.peass.vcs.GitCommit;
import de.dagere.peass.vcs.GitUtils;

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
   private final CommitIteratorGit iterator;

   public DependencyIteratorBuilder(final FixedCommitConfig executionConfig, final StaticTestSelection dependencies, final PeassFolders folders) {
      version = GitUtils.getName(executionConfig.getCommit() != null ? executionConfig.getCommit() : "HEAD", folders.getProjectFolder());

      String newestAnalyzedVersionName = dependencies != null ? dependencies.getNewestCommit() : null;

      String oldVersionCommit = getOldVersionCommit(executionConfig, newestAnalyzedVersionName, folders);

      if (version.equals(newestAnalyzedVersionName)) {
         LOG.info("Version {} is equal to newest version, not executing RTS", version);
         iterator = null;
         versionOld = getPrePredecessor(dependencies);
      } else if (oldVersionCommit.equals(version)) {
         LOG.error("Version {} is equal to predecessing version {}, some error occured - not executing RTS", version, oldVersionCommit);
         iterator = null;
         versionOld = dependencies.getNewestRunningCommit();
      } else {
         if (dependencies != null && 
               dependencies.getVersions().get(newestAnalyzedVersionName) != null && 
               !dependencies.getVersions().get(newestAnalyzedVersionName).isRunning()) {
            versionOld = newestAnalyzedVersionName;
            iterator = null;
         } else {
            GitCommit currentCommit = new GitCommit(version, "", "", "");
            List<String> commits = new LinkedList<>();
            commits.add(oldVersionCommit);
            commits.add(version);
            LOG.info("Analyzing {} - {}", oldVersionCommit, currentCommit);
            iterator = new CommitIteratorGit(folders.getProjectFolder(), commits, oldVersionCommit);
            versionOld = oldVersionCommit;
         }
      }
   }

   private String getPrePredecessor(final StaticTestSelection dependencies) {
      String[] versionNames = dependencies.getCommitNames();
      if (versionNames.length > 1) {
         String prePredecessor = versionNames[versionNames.length - 2];
         return prePredecessor;
      } else {
         return null;
      }
   }

   private static String getOldVersionCommit(final FixedCommitConfig executionConfig, final String newestRunningVersionName, final PeassFolders folders) {
      String oldVersion;
      if (executionConfig.getCommitOld() != null) {
         oldVersion = executionConfig.getCommitOld();
      } else if (newestRunningVersionName != null) {
         oldVersion = newestRunningVersionName;
      } else {
         oldVersion = GitUtils.getName("HEAD~1", folders.getProjectFolder());
      }
      return oldVersion;
   }

   public String getVersion() {
      return version;
   }

   public String getVersionOld() {
      return versionOld;
   }

   public CommitIteratorGit getIterator() {
      return iterator;
   }

}
