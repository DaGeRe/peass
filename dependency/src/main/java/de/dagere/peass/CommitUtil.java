package de.dagere.peass;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.vcs.GitCommit;
import de.peass.vcs.GitUtils;

public class CommitUtil {
   
   private static final Logger LOG = LogManager.getLogger(CommitUtil.class);
   
   public static List<GitCommit> getGitCommits(final String startversion, final String endversion, final File projectFolder) {
      final List<GitCommit> commits = GitUtils.getCommits(projectFolder, false);

      LOG.info("Processing git repo, commits: {}", commits.size());
      // LOG.debug("First Commits: {}", commits.subList(0, 10));
      if (startversion != null) {
         if (endversion != null) {
            GitUtils.filterList(startversion, endversion, commits);
         } else {
            GitUtils.filterList(startversion, null, commits);
            LOG.debug("First Commits: {}", commits.size() > 10 ? commits.subList(0, 10) : commits.subList(0, commits.size() - 1));
         }
      } else if (endversion != null) {
         GitUtils.filterList(null, endversion, commits);
      }
      LOG.info(commits);
      return commits;
   }
}
