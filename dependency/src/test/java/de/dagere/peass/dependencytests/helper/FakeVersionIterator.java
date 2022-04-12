package de.dagere.peass.dependencytests.helper;

import java.io.File;
import java.util.List;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.VersionDiff;
import de.dagere.peass.vcs.GitCommit;
import de.dagere.peass.vcs.VersionIterator;

/**
 * An iterator which only goes to the versions but does not change anything on the filesystem
 * @author reichelt
 *
 */
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
   public boolean goToPreviousCommit() {
      throw new RuntimeException("Not implemented on purpose - this is only a testing mock with limited functionality.");
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
   public VersionDiff getChangedClasses(final File projectFolder2, final List<File> genericModules, final String lastVersion, final ExecutionConfig config) {
      throw new RuntimeException("Not implemented on purpose - this is only a testing mock with limited functionality.");
   }

   @Override
   public String getPredecessor() {
      throw new RuntimeException("Not implemented yet.");
   }
}