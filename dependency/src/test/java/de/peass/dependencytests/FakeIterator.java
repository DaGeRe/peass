package de.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.apache.commons.io.FileUtils;

import de.peass.vcs.VersionIterator;

class CopyFileVisitor extends SimpleFileVisitor<Path> {
   private final Path targetPath;
   private Path sourcePath = null;

   public CopyFileVisitor(Path targetPath) {
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

public class FakeIterator extends VersionIterator {

   static void copy(File src, File dest) throws IOException {
      Files.walkFileTree(src.toPath(), new CopyFileVisitor(dest.toPath()));
   }

   List<File> commits;

   public FakeIterator(final File folder, final List<File> commits) {
      super(folder);
      this.commits = commits;
   }

   int tag = 0;

   @Override
   public int getSize() {
      return commits.size();
   }

   @Override
   public String getTag() {
      return "00000" + tag;
   }

   @Override
   public boolean hasNextCommit() {
      return tag < commits.size() + 1;
   }

   @Override
   public boolean goToNextCommit() {
      tag++;
      try {
         FileUtils.deleteDirectory(projectFolder);
         File commitFolder = commits.get(tag - 1);
         copy(commitFolder, projectFolder);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return true;
   }

   @Override
   public boolean goToFirstCommit() {
      tag = 0;
      return true;
   }

   @Override
   public boolean goTo0thCommit() {
      throw new RuntimeException("Not implemented on purpose.");
   }

   @Override
   public boolean isPredecessor(String lastRunningVersion) {
      return lastRunningVersion.equals("00000" + (tag - 1));
   }
}