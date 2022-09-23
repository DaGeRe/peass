package de.dagere.peass.dependencytests.helper;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.CommitDiff;
import de.dagere.peass.vcs.CommitIterator;

/**
 * An iterator which only goes to the versions but does not change anything on the filesystem
 * @author reichelt
 *
 */
public class FakeCommitIterator extends CommitIterator {

   List<String> commits;

   public FakeCommitIterator(final File folder, final List<String> commits) {
      super(folder);
      this.commits = commits;
   }

   @Override
   public int getSize() {
      return commits.size();
   }

   @Override
   public String getTag() {
      return commits.get(tagid);
   }

   @Override
   public boolean hasNextCommit() {
      return tagid < commits.size() - 1 ;
   }

   @Override
   public boolean goToNextCommit() {
      tagid++;
      return true;
   }

   @Override
   public boolean goToFirstCommit() {
      tagid = 0;
      return true;
   }
   
   @Override
   public boolean goToPreviousCommit() {
      tagid--;
      return true;
   }

   @Override
   public boolean goTo0thCommit() {
      throw new RuntimeException("Not implemented on purpose - this is only a testing mock with limited functionality.");
   }

   @Override
   public boolean isPredecessor(final String lastRunningVersion) {
      throw new RuntimeException("Not implemented on purpose - this is only a testing mock with limited functionality.");
   }
   
   @Override
   public CommitDiff getChangedClasses(final File projectFolder2, final List<File> genericModules, final String lastVersion, final ExecutionConfig config) {
      throw new RuntimeException("Not implemented on purpose - this is only a testing mock with limited functionality.");
   }

   @Override
   public String getPredecessor() {
      throw new RuntimeException("Not implemented yet.");
   }

   @Override
   public List<String> getCommits() {
      return new LinkedList<>();
   }

   @Override
   public boolean goToNamedCommit(String name) {
      while (!commits.get(tagid).equals(name) && tagid < commits.size()) {
         goToNextCommit();
      }
      return commits.get(tagid).equals(name);
   }
}