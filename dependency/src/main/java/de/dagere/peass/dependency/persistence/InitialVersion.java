package de.dagere.peass.dependency.persistence;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.data.TestSet.ChangedEntitityDeserializer;

public class InitialVersion {
   
   private String version;
   private int jdk = 8;

   @JsonDeserialize(keyUsing = ChangedEntitityDeserializer.class)
   private Map<ChangedEntity, InitialDependency> initialDependencies = new TreeMap<>();

   public String getVersion() {
      return version;
   }

   public void setVersion(final String version) {
      this.version = version;
   }

   public Map<ChangedEntity, InitialDependency> getInitialDependencies() {
      return initialDependencies;
   }

   public void setInitialDependencies(final Map<ChangedEntity, InitialDependency> initialDependencies) {
      this.initialDependencies = initialDependencies;
   }

   public int getJdk() {
      return jdk;
   }

   public void setJdk(final int jdk) {
      this.jdk = jdk;
   }
   
   public void addDependency(final ChangedEntity testcase, final ChangedEntity callee) {
      InitialDependency dependency = initialDependencies.get(testcase);  
      if (dependency == null) {
         dependency = new InitialDependency();  
         initialDependencies.put(testcase, dependency);
      }
      dependency.getEntities().add(callee);
   }

   @JsonIgnore
   public void sort(final ChangedEntity key) {
      Collections.sort(initialDependencies.get(key).getEntities());
   }
   
   @JsonIgnore
   public TestSet getInitialTests() {
      TestSet initialTests = new TestSet();
      for (ChangedEntity testEntity : initialDependencies.keySet()) {
         TestCase initialTest = new TestCase(testEntity);
         initialTests.addTest(initialTest);
      }
      return initialTests;
   }
}
