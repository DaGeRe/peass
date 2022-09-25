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
 * Identifies the commits that should be analyzed, by: (1) Using the commits from ExecutionConfig, if both are given and != null (resolving HEAD and HEAD~1 to the real commits)
 * (2) Using the commit from ExecutionConfig, if commit is given and commitOld is null, and using the *newest runnable commit* from Dependencies
 * 
 * In case (2), the iterator will only be set if analyzing is necessary, i.e. if the *newest commit* and commit differ. Otherwise, it will be null.
 *
 */
public class CommitIteratorBuilder {

   private static final Logger LOG = LogManager.getLogger(CommitIteratorBuilder.class);

   private final String commit, commitOld;

   public CommitIteratorBuilder(final FixedCommitConfig commitConfig, final StaticTestSelection staticTestSelection, final PeassFolders folders) {
      commit = GitUtils.getName(commitConfig.getCommit() != null ? commitConfig.getCommit() : "HEAD", folders.getProjectFolder());

      String newestAnalyzedCommitName = staticTestSelection != null ? staticTestSelection.getNewestCommit() : null;

      String oldCommit = getOldCommit(commitConfig, newestAnalyzedCommitName, folders, staticTestSelection);

      if (commit.equals(newestAnalyzedCommitName)) {
         LOG.info("Commit {} is equal to newest commit, not executing RTS", commit);
         commitOld = staticTestSelection.getCommits().get(newestAnalyzedCommitName).getPredecessor();
      } else if (oldCommit.equals(commit)) {
         LOG.error("Commit {} is equal to predecessing commit {}, some error occured - not executing RTS", commit, oldCommit);
         commitOld = staticTestSelection.getNewestRunningCommit();
      } else {
         if (staticTestSelection != null &&
               staticTestSelection.getCommits().get(newestAnalyzedCommitName) != null &&
               !staticTestSelection.getCommits().get(newestAnalyzedCommitName).isRunning()) {
            commitOld = newestAnalyzedCommitName;
         } else {
            GitCommit currentCommit = new GitCommit(commit, "", "", "");
            LOG.info("Analyzing {} - {}", oldCommit, currentCommit);
            commitOld = oldCommit;
         }
      }
   }

   private static String getOldCommit(final FixedCommitConfig executionConfig, final String newestRunningCommitName, final PeassFolders folders,
         StaticTestSelection staticSelection) {
      String oldCommit;
      if (executionConfig.getCommitOld() != null) {
         oldCommit = executionConfig.getCommitOld();
         if (oldCommit.equals("HEAD~1")) {
            oldCommit = GitUtils.getName("HEAD~1", folders.getProjectFolder());
         }
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
}
