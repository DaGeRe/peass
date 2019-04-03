package de.peass.parallel;

import java.util.LinkedList;
import java.util.List;

import de.peass.dependencyprocessors.VersionComparator;
import de.peass.vcs.GitCommit;

public class ParallelTestUtil {
   public static List<GitCommit> getCommits() {
      List<GitCommit> commits = new LinkedList<>();
      for (int i = 0; i < 10; i++) {
         GitCommit commit = new GitCommit(Integer.toString(i), "Test", null, "Test " + i);
         commits.add(commit);
      }
      VersionComparator.setVersions(commits);
      return commits;
   }
}
