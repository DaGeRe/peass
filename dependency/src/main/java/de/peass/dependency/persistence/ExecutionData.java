package de.peass.dependency.persistence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.analysis.data.TestSet.ChangedEntitityDeserializer;
import de.peass.dependencyprocessors.VersionComparator;

/**
 * Saves all tests where traces have changed and therefore a performance change could have taken place.
 * 
 * Used for JSON Serialisation.
 * 
 * @author reichelt
 *
 */
public class ExecutionData {

   private String url;
   private boolean isAndroid = false;

   private Map<String, TestSet> versions = new TreeMap<>(new VersionComparator());

   public String getUrl() {
      return url;
   }

   public void setUrl(final String url) {
      this.url = url;
   }

   public void setVersions(final Map<String, TestSet> versions) {
      this.versions = versions;
   }

   public Map<String, TestSet> getVersions() {
      return versions;
   }

   @JsonIgnore
   public void addCall(final String version, final TestSet tests) {
      final TestSet executes = versions.get(version);
      if (executes == null) {
         versions.put(version, tests);
      } else {
         executes.addTestSet(tests);
      }
   }

   @JsonIgnore
   public void addCall(final String version, final String predecessor, final TestCase testcase) {
      TestSet executes = versions.get(version);
      if (executes == null) {
         executes = new TestSet();
         versions.put(version, executes);
         executes.setPredecessor(predecessor);
      }
      if (!executes.getPredecessor().equals(predecessor)) {
         throw new RuntimeException("Unexpected: Different predecessor: " + predecessor + " " + executes.getPredecessor());
      }
      executes.addTest(testcase);
   }

   // @JsonIgnore
   // public void addCall(final String version, final TestCase testcase) {
   // TestSet executes = versions.get(version);
   // if (executes == null) {
   // executes = new TestSet();
   // versions.put(version, executes);
   // }
   // executes.addTest(testcase);
   // }

   @JsonIgnore
   public boolean versionContainsTest(final String version, final TestCase currentIterationTest) {
      final TestSet clazzExecutions = versions.get(version);
      if (clazzExecutions != null) {
         for (final Map.Entry<ChangedEntity, Set<String>> clazz : clazzExecutions.entrySet()) {
            final ChangedEntity testclazz = clazz.getKey();
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

         final List<String> versionNames = new LinkedList<>();
         versionNames.addAll(unsorted.keySet());
         Collections.sort(versionNames, new VersionComparator());

         for (final String version : versionNames) {
            versions.put(version, unsorted.get(version));
         }
      }
   }

   public void setAndroid(final boolean isAndroid) {
      this.isAndroid = isAndroid;
   }

   public boolean isAndroid() {
      return isAndroid;
   }

}