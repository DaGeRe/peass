package de.dagere.peass.ci;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.vcs.GitCommit;
import de.dagere.peass.vcs.VersionIteratorGit;

public class DependencyIteratorBuilder {
   
   private static final Logger LOG = LogManager.getLogger(ContinuousDependencyReader.class);
   
   public static VersionIteratorGit getIterator(final ExecutionConfig executionConfig, final String lastVersionName, final String versionName, final PeassFolders folders) {
      GitCommit currentCommit = new GitCommit(versionName, "", "", "");
      GitCommit lastAnalyzedCommit = new GitCommit(executionConfig.getVersionOld() != null ? executionConfig.getVersionOld() : lastVersionName, "", "", "");

      List<GitCommit> commits = new LinkedList<>();
      commits.add(lastAnalyzedCommit);
      commits.add(currentCommit);
      LOG.info("Analyzing {} - {}", lastAnalyzedCommit, currentCommit);
      VersionIteratorGit newIterator = new VersionIteratorGit(folders.getProjectFolder(), commits, lastAnalyzedCommit);
      
      return newIterator;
   }
}
