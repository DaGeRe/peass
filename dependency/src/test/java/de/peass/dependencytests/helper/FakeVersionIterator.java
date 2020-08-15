package de.peass.dependencytests.helper;

import java.io.File;
import java.util.List;

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
   
   @Override
   public boolean goToNextCommitSoft() {
      throw new RuntimeException("Not implemented on purpose.");
   }
}