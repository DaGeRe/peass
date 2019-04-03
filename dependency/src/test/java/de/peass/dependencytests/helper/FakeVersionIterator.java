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

import de.peass.vcs.GitCommit;
import de.peass.vcs.VersionIterator;

public class FakeVersionIterator extends VersionIterator {

   List<GitCommit> commits;

   public FakeVersionIterator(final File folder, final List<GitCommit> commits) {
      super(folder);
      this.commits = commits;
   }

   private int tag = 0;

   @Override
   public int getSize() {
      return commits.size();
   }

   @Override
   public String getTag() {
      return commits.get(tag).getTag();
   }

   @Override
   public boolean hasNextCommit() {
      return tag < commits.size() - 1 ;
   }

   @Override
   public boolean goToNextCommit() {
      tag++;
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
      throw new RuntimeException("Not implemented on purpose.");
   }
}