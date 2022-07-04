package de.dagere.peass.parallel;

import java.util.LinkedList;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.dependency.persistence.InitialCommit;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.reader.DependencyReaderUtil;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.vcs.GitCommit;

public class TestMerging {
   @Test
   public void testMerging() {
      CommitComparatorInstance comparator = ParallelTestUtil.getCommits();

      StaticTestSelection deps1 = new StaticTestSelection(), deps2 = new StaticTestSelection();
      deps1.setInitialcommit(new InitialCommit());
      deps1.getInitialcommit().setCommit("0");
      for (String commit : comparator.getCommits().subList(1, 6)) {
         deps1.getVersions().put(commit, null);
      }
      deps2.setInitialcommit(new InitialCommit());
      deps2.getInitialcommit().setCommit("5");
      for (String commit : comparator.getCommits().subList(5, 10)) {
         deps2.getVersions().put(commit, null);
      }
      StaticTestSelection merged = DependencyReaderUtil.mergeDependencies(deps1, deps2, comparator);
      Assert.assertEquals(9, merged.getVersions().size());
   }

   @Test
   public void testMergingStrangeDistribution() {
      CommitComparatorInstance comparator = ParallelTestUtil.getCommits();

      StaticTestSelection deps1 = new StaticTestSelection(), deps2 = new StaticTestSelection();
      deps1.setInitialcommit(new InitialCommit());
      deps1.getInitialcommit().setCommit("0");
      for (String commit : comparator.getCommits().subList(1, 8)) {
         deps1.getVersions().put(commit, null);
      }
      deps2.setInitialcommit(new InitialCommit());
      deps2.getInitialcommit().setCommit("7");
      for (String commit : comparator.getCommits().subList(8, 10)) {
         deps2.getVersions().put(commit, null);
      }
      StaticTestSelection merged = DependencyReaderUtil.mergeDependencies(deps1, deps2, comparator);
      Assert.assertEquals(9, merged.getVersions().size());
   }

   @Test
   public void testAlphabetic() {
      List<String> commits = new LinkedList<>();
      commits.add("A");
      commits.add("C");
      commits.add("B");
      commits.add("G");
      commits.add("E");
      commits.add("F");
      
      CommitComparatorInstance comparator = new CommitComparatorInstance(commits);
      
      StaticTestSelection deps1 = new StaticTestSelection(), deps2 = new StaticTestSelection();
      deps1.setInitialcommit(new InitialCommit());
      deps1.getInitialcommit().setCommit("A");
      deps1.getVersions().put("C", null);
      deps1.getVersions().put("B", null);
      deps1.getVersions().put("G", null);
      deps2.setInitialcommit(new InitialCommit());
      deps2.getInitialcommit().setCommit("G");
      deps2.getVersions().put("E", null);
      deps2.getVersions().put("F", null);
      StaticTestSelection merged = DependencyReaderUtil.mergeDependencies(deps1, deps2, comparator);
      System.out.println(merged.getVersions().keySet());
      Assert.assertEquals(5, merged.getVersions().size());
   }
}
