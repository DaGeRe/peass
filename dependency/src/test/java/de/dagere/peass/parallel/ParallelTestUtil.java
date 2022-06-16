package de.dagere.peass.parallel;

import java.util.LinkedList;
import java.util.List;

import de.dagere.peass.dependencyprocessors.VersionComparatorInstance;

public class ParallelTestUtil {
   public static VersionComparatorInstance getCommits() {
      List<String> commits = new LinkedList<>();
      for (int i = 0; i < 10; i++) {
         commits.add(Integer.toString(i));
      }
      return new VersionComparatorInstance(commits);
   }
}
