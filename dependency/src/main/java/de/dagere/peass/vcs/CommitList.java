package de.dagere.peass.vcs;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class CommitList {
   List<GitCommit> commits = new LinkedList<>();

   public List<GitCommit> getCommits() {
      return commits;
   }

   public void setCommits(List<GitCommit> commits) {
      this.commits = commits;
   }
   
   public void addCommits(List<GitCommit> newCommits) {
      Set<String> commitNames = commits.stream().map(commit -> commit.getTag()).collect(Collectors.toSet());
      for (GitCommit newCommit : newCommits) {
         if (!commitNames.contains(newCommit.getTag())) {
            commits.add(newCommit);
         }
      }
   }

   public GitCommit getCommit(String commitName) {
      for (GitCommit commit : commits) {
         if (commit.getTag().equals(commitName)) {
            return commit;
         }
      }
      return null;
   }
}