package de.dagere.peass.vcs;

import java.io.File;
import java.io.IOException;

import de.dagere.peass.utils.StreamGobbler;

public class ProjectBuilderHelper {
   public static void init(final File goalFolder) throws InterruptedException, IOException {
      final ProcessBuilder builder = new ProcessBuilder("git", "init");
      builder.directory(goalFolder);
      builder.start().waitFor();
   }

   public static void commit(final File goalFolder, final String message) throws InterruptedException, IOException {
      final ProcessBuilder builderAdd = new ProcessBuilder("git", "add", "-A");
      builderAdd.directory(goalFolder);
      StreamGobbler.showFullProcess(builderAdd.start());
      final ProcessBuilder builder = new ProcessBuilder("git", "commit", "-m", message);
      builder.directory(goalFolder);
      StreamGobbler.showFullProcess(builder.start());
   }
   
   public static void branch(final File goalFolder, final String branchName) throws InterruptedException, IOException {
      final ProcessBuilder builder = new ProcessBuilder("git", "branch", branchName);
      builder.directory(goalFolder);
      builder.start().waitFor();
   }
   
   public static String getBranch(final File goalFolder) throws InterruptedException, IOException {
      final ProcessBuilder builder = new ProcessBuilder("git", "branch");
      builder.directory(goalFolder);
      builder.start().waitFor();
      return "";
   }
   
   public static void checkout(final File goalFolder, final String branchName) throws InterruptedException, IOException {
      final ProcessBuilder builder = new ProcessBuilder("git", "checkout", branchName);
      builder.directory(goalFolder);
      builder.start().waitFor();
   }
   
   public static void merge(final File goalFolder, final String branchName) throws InterruptedException, IOException {
      final ProcessBuilder builder = new ProcessBuilder("git", "merge", branchName);
      builder.directory(goalFolder);
      StreamGobbler.showFullProcess(builder.start());
   }
   
   public static void mergeTheirs(final File goalFolder, final String branchName) throws InterruptedException, IOException {
      final ProcessBuilder builder = new ProcessBuilder("git",  "merge", "-X", "theirs", branchName, "-m", "Merge using theirs");
      builder.directory(goalFolder);
      StreamGobbler.showFullProcess(builder.start());
   }
}
