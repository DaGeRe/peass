package de.peass.dependencytests.helper;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.apache.commons.io.FileUtils;

import de.peass.dependency.analysis.data.VersionDiff;
import de.peass.vcs.VersionIterator;

class CopyFileVisitor extends SimpleFileVisitor<Path> {
   private final Path targetPath;
   private Path sourcePath = null;

   public CopyFileVisitor(final Path targetPath) {
      this.targetPath = targetPath;
   }

   @Override
   public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
      if (sourcePath == null) {
         sourcePath = dir;
      }
      Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
      return FileVisitResult.CONTINUE;
   }

   @Override
   public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
      Files.copy(file, targetPath.resolve(sourcePath.relativize(file)));
      return FileVisitResult.CONTINUE;
   }
}

public class FakeFileIterator extends VersionIterator {

   public static void copy(final File src, final File dest) throws IOException {
      Files.walkFileTree(src.toPath(), new CopyFileVisitor(dest.toPath()));
   }

   private List<File> commits;
   private final int tagDiff;

   public FakeFileIterator(final File folder, final List<File> commits) {
      super(folder);
      this.commits = commits;
      tagDiff = 0;
   }

   public FakeFileIterator(final File folder, final List<File> commits, final int tagDiff) {
      super(folder);
      this.commits = commits;
      this.tagDiff = tagDiff;
   }

   private int tag = 0;

   @Override
   public int getSize() {
      return commits.size();
   }

   @Override
   public String getTag() {
      return "00000" + (tag + tagDiff);
   }

   @Override
   public boolean hasNextCommit() {
      return tag + tagDiff < commits.size();
   }

   @Override
   public boolean goToNextCommit() {
      System.out.println("Loading commit: " + tag);
      tag++;
      return loadVersionFiles(tag - 1);
   }

   @Override
   public boolean goToFirstCommit() {
      tag = 1;
      return loadVersionFiles(0);
   }

   @Override
   public boolean goToPreviousCommit() {
      if (tag > 1) {
         tag--;
         return loadVersionFiles(tag - 1);
      } else {
         return false;
      }
   }

   private boolean loadVersionFiles(final int goalTag) {
      try {
         FileUtils.deleteDirectory(projectFolder);
         File commitFolder = commits.get(goalTag);
         copy(commitFolder, projectFolder);
         return true;
      } catch (final IOException e) {
         e.printStackTrace();
         return false;
      }
   }

   @Override
   public boolean goTo0thCommit() {
      tag = -1;
      return loadVersionFiles(0);
   }

   @Override
   public boolean isPredecessor(final String lastRunningVersion) {
      return lastRunningVersion.equals("00000" + (tag - 1 + tagDiff));
   }

   @Override
   public boolean goToNextCommitSoft() {
      throw new RuntimeException("Not implemented on purpose.");
   }

   @Override
   public VersionDiff getChangedClasses(final File projectFolder, final List<File> genericModules, final String lastVersion) {
      throw new RuntimeException("Not implemented yet.");
   }

   @Override
   public String getPredecessor() {
      throw new RuntimeException("Not implemented yet.");
   }
}