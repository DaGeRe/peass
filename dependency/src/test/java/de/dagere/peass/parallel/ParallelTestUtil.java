package de.dagere.peass.parallel;

import java.util.LinkedList;
import java.util.List;

import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.vcs.GitCommit;

public class ParallelTestUtil {
   public static List<String> getCommits() {
      List<String> commits = new LinkedList<>();
      for (int i = 0; i < 10; i++) {
//         GitCommit commit = new GitCommit(Integer.toString(i), "Test", null, "Test " + i);
         commits.add(Integer.toString(i));
      }
      VersionComparator.setVersions(commits);
      return commits;
   }
}
