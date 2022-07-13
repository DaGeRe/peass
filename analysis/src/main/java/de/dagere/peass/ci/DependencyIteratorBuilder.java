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

   private final String commit, commitOld;
   private final CommitIteratorGit iterator;

   public DependencyIteratorBuilder(final FixedCommitConfig executionConfig, final StaticTestSelection dependencies, final PeassFolders folders) {
      commit = GitUtils.getName(executionConfig.getCommit() != null ? executionConfig.getCommit() : "HEAD", folders.getProjectFolder());

      String newestAnalyzedCommitName = dependencies != null ? dependencies.getNewestCommit() : null;

      String oldCommit = getOldCommit(executionConfig, newestAnalyzedCommitName, folders, dependencies);

      if (commit.equals(newestAnalyzedCommitName)) {
         LOG.info("Commit {} is equal to newest commit, not executing RTS", commit);
         iterator = null;
         commitOld = dependencies.getCommits().get(newestAnalyzedCommitName).getPredecessor();
      } else if (oldCommit.equals(commit)) {
         LOG.error("Commit {} is equal to predecessing commit {}, some error occured - not executing RTS", commit, oldCommit);
         iterator = null;
         commitOld = dependencies.getNewestRunningCommit();
      } else {
         if (dependencies != null &&
               dependencies.getCommits().get(newestAnalyzedCommitName) != null &&
               !dependencies.getCommits().get(newestAnalyzedCommitName).isRunning()) {
            commitOld = newestAnalyzedCommitName;
            iterator = null;
         } else {
            GitCommit currentCommit = new GitCommit(commit, "", "", "");
            List<String> commits = new LinkedList<>();
            commits.add(oldCommit);
            commits.add(commit);
            LOG.info("Analyzing {} - {}", oldCommit, currentCommit);
            iterator = new CommitIteratorGit(folders.getProjectFolder(), commits, oldCommit);
            commitOld = oldCommit;
         }
      }
   }

   private static String getOldCommit(final FixedCommitConfig executionConfig, final String newestRunningCommitName, final PeassFolders folders,
         StaticTestSelection staticSelection) {
      String oldCommit;
      if (executionConfig.getCommitOld() != null) {
         oldCommit = executionConfig.getCommitOld();
      } else if (newestRunningCommitName != null) {
         oldCommit = newestRunningCommitName;
      } else {
         oldCommit = GitUtils.getName("HEAD~1", folders.getProjectFolder());
      }
      return oldCommit;
   }

   public String getCommit() {
      return commit;
   }

   public String getCommitOld() {
      return commitOld;
   }

   public CommitIteratorGit getIterator() {
      return iterator;
   }

}
