package de.dagere.peass.vcs;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.utils.StreamGobbler;

public class ProjectBuilderHelper {
   
   private static final Logger LOG = LogManager.getLogger(ProjectBuilderHelper.class);
   
   public static void init(final File goalFolder) throws InterruptedException, IOException {
      LOG.debug("Initializing git repo");
      final ProcessBuilder builder = new ProcessBuilder("git", "init");
      builder.directory(goalFolder);
      builder.start().waitFor();
   }

   public static void commit(final File goalFolder, final String message) throws InterruptedException, IOException {
      LOG.debug("Adding files");
      final ProcessBuilder builderAdd = new ProcessBuilder("git", "add", "-A");
      builderAdd.directory(goalFolder);
      StreamGobbler.showFullProcess(builderAdd.start());

      LOG.debug("Creating commit");
      final ProcessBuilder builder = new ProcessBuilder("git", "-c", "user.name='Anonym'",
            "-c", "user.email='anonym@generated.org'",
            "commit", "-m", message);
      builder.directory(goalFolder);
      StreamGobbler.showFullProcess(builder.start());
   }

   public static void branch(final File goalFolder, final String branchName) throws InterruptedException, IOException {
      LOG.debug("Creating branch {}", branchName);
      final ProcessBuilder builder = new ProcessBuilder("git", "branch", branchName);
      builder.directory(goalFolder);
      builder.start().waitFor();
   }

   public static void checkout(final File goalFolder, final String branchName) throws InterruptedException, IOException {
      LOG.debug("Checking out {}", branchName);
      final ProcessBuilder builder = new ProcessBuilder("git", "checkout", branchName);
      builder.directory(goalFolder);
      builder.start().waitFor();
   }

   public static void merge(final File goalFolder, final String branchName) throws InterruptedException, IOException {
      LOG.debug("Doing simple merge of {}", branchName);
      final ProcessBuilder builder = new ProcessBuilder("git", "merge", branchName);
      builder.directory(goalFolder);
      StreamGobbler.showFullProcess(builder.start());
   }

   public static void mergeTheirs(final File goalFolder, final String branchName) throws InterruptedException, IOException {
      LOG.debug("Merging with theirs strategy of branch {}", branchName);
      final ProcessBuilder builder = new ProcessBuilder("git", "-c", "user.name='Anonym'",
            "-c", "user.email='anonym@generated.org'",
            "merge", "-X", "theirs", branchName, "-m", "Merge using theirs");
      builder.directory(goalFolder);
      StreamGobbler.showFullProcess(builder.start());
   }
}
