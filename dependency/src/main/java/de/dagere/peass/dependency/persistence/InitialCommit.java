package de.dagere.peass.dependency.persistence;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.data.deserializer.TestMethodCallKeyDeserializer;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;

public class InitialCommit {

   private String commit;
   
   // To asure compatibility to old versions, this field still needs to stay here; but in all future serializations, it should be replaced by commit
   @Deprecated
   @JsonInclude(value = JsonInclude.Include.NON_NULL)
   private String version;

   private int jdk = 8;
   private boolean running = true;

   @JsonDeserialize(keyUsing = TestMethodCallKeyDeserializer.class)
   private Map<TestMethodCall, InitialCallList> initialDependencies = new TreeMap<>();

   public String getCommit() {
      return commit;
   }

   public void setCommit(String commit) {
      this.commit = commit;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(final String version) {
      this.version = null;
      this.commit = version;
   }

   @JsonInclude(value = Include.CUSTOM, valueFilter = IsRunningFilter.class)
   public boolean isRunning() {
      return running;
   }

   public void setRunning(boolean running) {
      this.running = running;
   }

   public Map<TestMethodCall, InitialCallList> getInitialDependencies() {
      return initialDependencies;
   }

   public void setInitialDependencies(final Map<TestMethodCall, InitialCallList> initialDependencies) {
      this.initialDependencies = initialDependencies;
   }

   public int getJdk() {
      return jdk;
   }

   public void setJdk(final int jdk) {
      this.jdk = jdk;
   }

   public void addDependency(final TestMethodCall testcase, final ChangedEntity callee) {
      InitialCallList dependency = initialDependencies.get(testcase);
      if (dependency == null) {
         dependency = new InitialCallList();
         initialDependencies.put(testcase, dependency);
      }
      dependency.getEntities().add(callee);
   }

   @JsonIgnore
   public void sort(final TestCase key) {
      Collections.sort(initialDependencies.get(key).getEntities());
   }

   @JsonIgnore
   public TestSet getInitialTests() {
      TestSet initialTests = new TestSet();
      for (TestCase testEntity : initialDependencies.keySet()) {
         initialTests.addTest(testEntity);
      }
      return initialTests;
   }

   private static final class IsRunningFilter {
      @Override
      public boolean equals(Object obj) {
         if (obj == null || !(obj instanceof Boolean)) {
            return false;
        }
        final Boolean v = (Boolean) obj;
        return Boolean.TRUE.equals(v);
      }
   }
}
