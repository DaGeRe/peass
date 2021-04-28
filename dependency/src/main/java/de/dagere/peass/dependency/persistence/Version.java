package de.dagere.peass.dependency.persistence;

import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.data.TestSet.ChangedEntitityDeserializer;

public class Version {

   private boolean running;
   private int jdk = 8;
   private String predecessor;
   
   @JsonDeserialize(keyUsing = ChangedEntitityDeserializer.class)
   private Map<ChangedEntity, TestSet> changedClazzes = new TreeMap<>();

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

   public Map<ChangedEntity, TestSet> getChangedClazzes() {
      return changedClazzes;
   }

   public void setChangedClazzes(final Map<ChangedEntity, TestSet> changedClazzes) {
      this.changedClazzes = changedClazzes;
   }
   
   @JsonIgnore
   public TestSet getTests() {
//      Iterator<TestSet> tests = changedClazzes.values().iterator();
      final TestSet union = new TestSet();
      for (final TestSet current : changedClazzes.values()) {
         union.addTestSet(current);
      }
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
