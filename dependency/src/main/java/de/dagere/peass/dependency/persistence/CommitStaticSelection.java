package de.dagere.peass.dependency.persistence;

import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.dagere.nodeDiffGenerator.data.MethodCall;
import de.dagere.nodeDiffGenerator.data.serialization.MethodCallDeserializer;
import de.dagere.peass.dependency.analysis.data.TestSet;

public class CommitStaticSelection {

   private boolean running;
   private int jdk = 8;
   private String predecessor;
   
   @JsonDeserialize(keyUsing = MethodCallDeserializer.class)
   private Map<MethodCall, TestSet> changedClazzes = new TreeMap<>();
   
   private TestSet ignoredAffectedTests = null;
   private TestSet removedTests = null;

   public boolean isRunning() {
      return running;
   }

   public void setRunning(final boolean running) {
      this.running = running;
   }

   public int getJdk() {
      return jdk;
   }

   public void setJdk(final int jdk) {
      this.jdk = jdk;
   }

   public Map<MethodCall, TestSet> getChangedClazzes() {
      return changedClazzes;
   }

   public void setChangedClazzes(final Map<MethodCall, TestSet> changedClazzes) {
      this.changedClazzes = changedClazzes;
   }
   
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public TestSet getIgnoredAffectedTests() {
      return ignoredAffectedTests;
   }
   
   public void setIgnoredAffectedTests(TestSet ignoredAffectedTests) {
      this.ignoredAffectedTests = ignoredAffectedTests;
   }
   
   @JsonInclude(JsonInclude.Include.NON_NULL)
   public TestSet getRemovedTests() {
      return removedTests;
   }
   
   public void setRemovedTests(TestSet removedTests) {
      this.removedTests = removedTests;
   }
   
   @JsonIgnore
   public TestSet getTests() {
      final TestSet union = new TestSet();
      for (final TestSet current : changedClazzes.values()) {
         union.addTestSet(current);
      }
      union.setPredecessor(predecessor);
      return union;
   }

   @JsonInclude(Include.NON_EMPTY)
   public String getPredecessor() {
      return predecessor;
   }

   public void setPredecessor(final String predecessor) {
      this.predecessor = predecessor;
   }
}
