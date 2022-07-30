package de.dagere.peass.dependencyprocessors;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import de.dagere.peass.dependency.persistence.SelectedTests;

public class CommitComparatorInstance implements Comparator<String> {
   
   public static final String NO_BEFORE = "NO_BEFORE";
   
   private final List<String> commits;

   public CommitComparatorInstance(List<String> commits) {
      this.commits = commits;
   }
   
   public CommitComparatorInstance(final SelectedTests dependencies2) {
      commits = new LinkedList<>();
      Arrays.stream(dependencies2.getCommitNames()).forEach(commit -> commits.add(commit));
   }

   @Override
   public int compare(String commit1, String commit2) {
      final int indexOf = commits.indexOf(commit1);
      final int indexOf2 = commits.indexOf(commit2);
      return indexOf - indexOf2;
   }
   
   public boolean isBefore(final String commit1, final String commit2) {
      final int indexOf = commits.indexOf(commit1);
      final int indexOf2 = commits.indexOf(commit2);
      return indexOf < indexOf2;
   }

   public List<String> getCommits() {
      return commits;
   }

   public int getVersionIndex(String commit) {
      return commits.indexOf(commit);
   }

   public String getPreviousVersion(final String commit) {
      final int index = commits.indexOf(commit);
      return index > 0 ? commits.get(index - 1) : NO_BEFORE;
   }
}
