package de.dagere.peass.parallel;

import java.util.LinkedList;
import java.util.List;

import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;

public class ParallelTestUtil {
   public static CommitComparatorInstance getCommits() {
      List<String> commits = new LinkedList<>();
      for (int i = 0; i < 10; i++) {
         commits.add(Integer.toString(i));
      }
      return new CommitComparatorInstance(commits);
   }
}
