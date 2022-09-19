package de.dagere.peass.dependency.persistence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.testData.TestClazzCall;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependencyprocessors.VersionComparator;

/**
 * Saves all tests where traces have changed and therefore a performance change could have taken place.
 * 
 * Used for JSON Serialisation.
 * 
 * @author reichelt
 *
 */
public class ExecutionData extends SelectedTests {

   private Map<String, TestSet> commits = new LinkedHashMap<>();

   public ExecutionData() {
   }
   
   public ExecutionData(final StaticTestSelection dependencies) {
      setUrl(dependencies.getUrl());
      commits.put(dependencies.getInitialcommit().getCommit(), new TestSet());
      for (Map.Entry<String, CommitStaticSelection> commit : dependencies.getCommits().entrySet()) {
         TestSet tests = commit.getValue().getTests();
         commits.put(commit.getKey(), tests);
      }
   }
   
   public void setCommits(final Map<String, TestSet> commits) {
      this.commits = commits;
   }

   public Map<String, TestSet> getCommits() {
      return commits;
   }
   
   @JsonIgnore
   public void addEmptyCommit(final String commit, final String predecessor) {
      TestSet tests = new TestSet();
      tests.setPredecessor(predecessor);
      commits.put(commit, tests);
   }

   @JsonIgnore
   public void addCall(final String commit, final TestSet tests) {
      final TestSet executes = commits.get(commit);
      if (executes == null) {
         commits.put(commit, tests);
      } else {
         executes.addTestSet(tests);
      }
   }

   @JsonIgnore
   public void addCall(final String commit, final TestMethodCall testcase) {
      TestSet executes = commits.get(commit);
      if (executes == null) {
         executes = new TestSet();
         commits.put(commit, executes);
      }
      executes.addTest(testcase);
   }

   @JsonIgnore
   public boolean commitContainsTest(final String commit, final TestMethodCall currentIterationTest) {
      final TestSet clazzExecutions = commits.get(commit);
      if (clazzExecutions != null) {
         for (final Entry<TestClazzCall, Set<String>> clazz : clazzExecutions.entrySet()) {
            final TestCase testclazz = clazz.getKey();
            final Set<String> methods = clazz.getValue();
            if (testclazz.getClazz().equals(currentIterationTest.getClazz()) && methods.contains(currentIterationTest.getMethod())) {
               return true;
            }
         }
      }
      return false;
   }

   @JsonIgnore
   public void sort() {
      final Map<String, TestSet> unsorted = new LinkedHashMap<>();
      synchronized (commits) {
         unsorted.putAll(commits);
         commits.clear();

         final List<String> commitNames = new LinkedList<>();
         commitNames.addAll(unsorted.keySet());
         Collections.sort(commitNames, new VersionComparator());

         for (final String commit : commitNames) {
            commits.put(commit, unsorted.get(commit));
         }
      }
   }
   
   @JsonIgnore
   public int getAllExecutions() {
         int count2 = 0;
         for (final Entry<String, TestSet> entry : getCommits().entrySet()) {
            count2 += entry.getValue().getTestMethods().size();
         }
         return count2;
   }

   @JsonIgnore
   @Override
   public String[] getCommitNames() {
      return commits.keySet().toArray(new String[0]);
   }

}