package de.dagere.peass.dependency.persistence;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;

public class StaticTestSelection extends SelectedTests {

   private String testGoal;
   private InitialCommit initialcommit = new InitialCommit();
   
   @JsonInclude(value = JsonInclude.Include.NON_NULL)
   private InitialCommit initialversion = null;
   
   
   private Map<String, CommitStaticSelection> commits = new LinkedHashMap<>();

   public StaticTestSelection() {

   }

   public StaticTestSelection(final ExecutionData executiondata) {
      setUrl(executiondata.getUrl());
      // ExecutionData contain an empty first analyzed version; therefore, the initialversion of the dependencies is this first version
      String first = executiondata.getCommits().keySet().iterator().next();
      initialcommit.setCommit(first);
      for (Map.Entry<String, TestSet> commit : executiondata.getCommits().entrySet()) {
         if (!commit.getKey().equals(first)) {
            CommitStaticSelection commitDependencies = new CommitStaticSelection();
            commitDependencies.setPredecessor(commit.getValue().getPredecessor());
            commitDependencies.getChangedClazzes().put(new ChangedEntity("unknown", ""), commit.getValue());
            String commitHash = commit.getKey();
            commits.put(commitHash, commitDependencies);
            for (TestMethodCall test : commit.getValue().getTestMethods()) {
               initialcommit.addDependency(test, new ChangedEntity(test.getClazz(), ""));
            }
         }
      }
   }

   public void setTestGoal(final String testGoal) {
      this.testGoal = testGoal;
   }

   public String getTestGoal() {
      return testGoal;
   }
   
   // These getters/setters are only present for old data deserialization
   @Deprecated
   public InitialCommit getInitialversion() {
      return initialversion;
   }

   @Deprecated
   public void setInitialversion(final InitialCommit initialversion) {
      this.initialversion = initialversion;
   }

   public InitialCommit getInitialcommit() {
      // This hack asures that data with the old name initialversion are copied to initialcommit; it will obviusly not work if getInitialcommit is not called 
      if (initialversion != null) {
         initialcommit = initialversion;
         initialversion = null;
      }
      return initialcommit;
   }

   public void setInitialcommit(final InitialCommit initialversion) {
      this.initialcommit = initialversion;
   }

   public Map<String, CommitStaticSelection> getCommits() {
      return commits;
   }

   public void setCommits(final Map<String, CommitStaticSelection> versions) {
      this.commits = versions;
   }

   @JsonIgnore
   public String[] getCommitNames() {
      final String[] commitNames = commits.keySet().toArray(new String[0]);
      String[] withStartcommit = new String[commitNames.length + 1];
      withStartcommit[0] = initialcommit.getCommit();
      System.arraycopy(commitNames, 0, withStartcommit, 1, commitNames.length);
      return withStartcommit;
   }

   @JsonIgnore
   public String getNewestCommit() {
      final String[] commits = getCommitNames();
      if (commits.length > 0) {
         return commits[commits.length - 1];
      } else if (initialcommit != null) {
         return initialcommit.getCommit();
      } else {
         return null;
      }
   }

   @JsonIgnore
   public String[] getRunningCommitNames() {
      String[] commitNames = commits.entrySet().stream()
            .filter((entry) -> entry.getValue().isRunning())
            .map(entry -> entry.getKey())
            .toArray(String[]::new);

      String[] withStartcommit = new String[commitNames.length + 1];
      withStartcommit[0] = initialcommit.getCommit();
      System.arraycopy(commitNames, 0, withStartcommit, 1, commitNames.length);
      return withStartcommit;
   }

   @JsonIgnore
   public String getNewestRunningCommit() {
      final String[] commits = getRunningCommitNames();
      if (commits.length > 0) {
         return commits[commits.length - 1];
      } else if (initialcommit != null) {
         return initialcommit.getCommit();
      } else {
         return null;
      }

   }

   @JsonIgnore
   public ExecutionData toExecutionData() {
      ExecutionData executionData = new ExecutionData();
      for (Entry<String, CommitStaticSelection> entry : commits.entrySet()) {
         TestSet tests = entry.getValue().getTests();
         executionData.getCommits().put(entry.getKey(), tests);
      }
      return executionData;
   }
}
