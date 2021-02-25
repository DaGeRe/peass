package de.peass.dependency.persistence;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;

public class Dependencies extends SelectedTests {

   private InitialVersion initialversion = new InitialVersion();
   private Map<String, Version> versions = new LinkedHashMap<>();

   public Dependencies() {

   }

   public Dependencies(final ExecutionData executiondata) {
      setUrl(executiondata.getUrl());
      String first = executiondata.getVersions().values().iterator().next().getPredecessor();
      initialversion.setVersion(first);
      for (Map.Entry<String, TestSet> version : executiondata.getVersions().entrySet()) {
         Version version2 = new Version();
         version2.setPredecessor(version.getValue().getPredecessor());
         version2.getChangedClazzes().put(new ChangedEntity("unknown", ""), version.getValue());
         versions.put(version.getKey(), version2);
         for (TestCase test : version.getValue().getTests()) {
            initialversion.addDependency(new ChangedEntity(test.getClazz(), "", test.getMethod()), new ChangedEntity(test.getClazz(), ""));
         }
      }
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
      return versionNames;
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
}
