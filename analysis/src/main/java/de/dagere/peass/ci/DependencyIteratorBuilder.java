package de.dagere.peass.ci;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.vcs.GitCommit;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionIteratorGit;

public class DependencyIteratorBuilder {

   private static final Logger LOG = LogManager.getLogger(DependencyIteratorBuilder.class);

   private final String version, versionOld;
   private final VersionIteratorGit iterator;

   public DependencyIteratorBuilder(final ExecutionConfig executionConfig, final String newestRunningVersionName, final PeassFolders folders) {
      version = GitUtils.getName(executionConfig.getVersion() != null ? executionConfig.getVersion() : "HEAD", folders.getProjectFolder());

      GitCommit currentCommit = new GitCommit(version, "", "", "");
      GitCommit oldVersionCommit = getOldVersionCommit(executionConfig, newestRunningVersionName, folders);
      versionOld = oldVersionCommit.getTag();

      if (version.equals(newestRunningVersionName)) {
         LOG.info("Version {} is equal to newest version, not executing RTS", version);
         iterator = null;
      } else if (oldVersionCommit.getTag().equals(currentCommit.getTag())) {
         LOG.error("Version {} is equal to predecessing version {}, some error occured - not executing RTS", currentCommit.getTag(), oldVersionCommit.getTag());
         iterator = null;
      } else {
         List<GitCommit> commits = new LinkedList<>();
         commits.add(oldVersionCommit);
         commits.add(currentCommit);
         LOG.info("Analyzing {} - {}", oldVersionCommit, currentCommit);
         iterator = new VersionIteratorGit(folders.getProjectFolder(), commits, oldVersionCommit);
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
