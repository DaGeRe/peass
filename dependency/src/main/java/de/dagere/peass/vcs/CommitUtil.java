package de.dagere.peass.vcs;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommitUtil {
   
   private static final Logger LOG = LogManager.getLogger(CommitUtil.class);
   
   public static List<GitCommit> getGitCommits(final String startcommit, final String endcommit, final File projectFolder) {
      final List<GitCommit> commits = GitUtils.getCommits(projectFolder, false);

      LOG.info("Processing git repo, commits: {}", commits.size());
      // LOG.debug("First Commits: {}", commits.subList(0, 10));
      if (startcommit != null) {
         if (endcommit != null) {
            GitUtils.filterList(startcommit, endcommit, commits);
         } else {
            GitUtils.filterList(startcommit, null, commits);
            LOG.debug("First Commits: {}", commits.size() > 10 ? commits.subList(0, 10) : commits.subList(0, commits.size() - 1));
         }
      } else if (endcommit != null) {
         GitUtils.filterList(null, endcommit, commits);
      }
      LOG.info(commits);
      return commits;
   }
}
