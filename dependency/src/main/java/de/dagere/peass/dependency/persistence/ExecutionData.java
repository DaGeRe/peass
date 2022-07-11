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

   private Map<String, TestSet> versions = new LinkedHashMap<>();

   public ExecutionData() {
   }
   
   public ExecutionData(final StaticTestSelection dependencies) {
      setUrl(dependencies.getUrl());
      versions.put(dependencies.getInitialcommit().getCommit(), new TestSet());
      for (Map.Entry<String, CommitStaticSelection> version : dependencies.getVersions().entrySet()) {
         TestSet tests = version.getValue().getTests();
         versions.put(version.getKey(), tests);
      }
   }
   
   public void setVersions(final Map<String, TestSet> versions) {
      this.versions = versions;
   }

   public Map<String, TestSet> getVersions() {
      return versions;
   }
   
   @JsonIgnore
   public void addEmptyCommit(final String version, final String predecessor) {
      TestSet tests = new TestSet();
      tests.setPredecessor(predecessor);
      versions.put(version, tests);
   }

   @JsonIgnore
   public void addCall(final String commit, final TestSet tests) {
      final TestSet executes = versions.get(commit);
      if (executes == null) {
         versions.put(commit, tests);
      } else {
         executes.addTestSet(tests);
      }
   }

   @JsonIgnore
   public void addCall(final String commit, final TestCase testcase) {
      TestSet executes = versions.get(commit);
      if (executes == null) {
         executes = new TestSet();
         versions.put(commit, executes);
      }
      executes.addTest(testcase);
   }

   @JsonIgnore
   public boolean commitContainsTest(final String commit, final TestCase currentIterationTest) {
      final TestSet clazzExecutions = versions.get(commit);
      if (clazzExecutions != null) {
         for (final Map.Entry<TestCase, Set<String>> clazz : clazzExecutions.entrySet()) {
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
      synchronized (versions) {
         unsorted.putAll(versions);
         versions.clear();

         final List<String> commitNames = new LinkedList<>();
         commitNames.addAll(unsorted.keySet());
         Collections.sort(commitNames, new VersionComparator());

         for (final String version : commitNames) {
            versions.put(version, unsorted.get(version));
         }
      }
   }
   
   @JsonIgnore
   public int getAllExecutions() {
         int count2 = 0;
         for (final Entry<String, TestSet> entry : getVersions().entrySet()) {
            count2 += entry.getValue().getTests().size();
         }
         return count2;
   }

   @JsonIgnore
   @Override
   public String[] getCommitNames() {
      return versions.keySet().toArray(new String[0]);
   }

}