package de.dagere.peass.dependency.persistence;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;

public class Dependencies extends SelectedTests {

   private String testGoal;
   private InitialVersion initialversion = new InitialVersion();
   private Map<String, Version> versions = new LinkedHashMap<>();

   public Dependencies() {

   }

   public Dependencies(final ExecutionData executiondata) {
      setUrl(executiondata.getUrl());
      String first = executiondata.getVersions().values().iterator().next().getPredecessor();
      initialversion.setVersion(first);
      for (Map.Entry<String, TestSet> version : executiondata.getVersions().entrySet()) {
         Version versionDependencies = new Version();
         versionDependencies.setPredecessor(version.getValue().getPredecessor());
         versionDependencies.getChangedClazzes().put(new ChangedEntity("unknown", ""), version.getValue());
         String versionHash = version.getKey();
         versions.put(versionHash, versionDependencies);
         for (TestCase test : version.getValue().getTests()) {
            initialversion.addDependency(test, new ChangedEntity(test.getClazz(), ""));
         }
      }
   }

   public void setTestGoal(final String testGoal) {
      this.testGoal = testGoal;
   }

   public String getTestGoal() {
      return testGoal;
   }

   public InitialVersion getInitialversion() {
      return initialversion;
   }

   public void setInitialversion(final InitialVersion initialversion) {
      this.initialversion = initialversion;
   }

   public Map<String, Version> getVersions() {
      return versions;
   }

   public void setVersions(final Map<String, Version> versions) {
      this.versions = versions;
   }

   @JsonIgnore
   public String[] getVersionNames() {
      final String[] versionNames = versions.keySet().toArray(new String[0]);
      String[] withStartversion = new String[versionNames.length + 1];
      withStartversion[0] = initialversion.getVersion();
      System.arraycopy(versionNames, 0, withStartversion, 1, versionNames.length);
      return withStartversion;
   }

   @JsonIgnore
   public String getNewestVersion() {
      final String[] versions = getVersionNames();
      if (versions.length > 0) {
         return versions[versions.length - 1];
      } else if (initialversion != null) {
         return initialversion.getVersion();
      } else {
         return null;
      }
   }

   @JsonIgnore
   public String[] getRunningVersionNames() {
      String[] versionNames = versions.entrySet().stream()
            .filter((entry) -> entry.getValue().isRunning())
            .map(entry -> entry.getKey())
            .toArray(String[]::new);

      String[] withStartversion = new String[versionNames.length + 1];
      withStartversion[0] = initialversion.getVersion();
      System.arraycopy(versionNames, 0, withStartversion, 1, versionNames.length);
      return withStartversion;
   }

   @JsonIgnore
   public String getNewestRunningVersion() {
      final String[] versions = getRunningVersionNames();
      if (versions.length > 0) {
         return versions[versions.length - 1];
      } else if (initialversion != null) {
         return initialversion.getVersion();
      } else {
         return null;
      }

   }

   @JsonIgnore
   public ExecutionData toExecutionData() {
      ExecutionData executionData = new ExecutionData();
      for (Entry<String, Version> entry : versions.entrySet()) {
         TestSet tests = entry.getValue().getTests();
         executionData.getVersions().put(entry.getKey(), tests);
      }
      return executionData;
   }
}
