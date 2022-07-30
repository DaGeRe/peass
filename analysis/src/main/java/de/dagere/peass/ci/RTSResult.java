package de.dagere.peass.ci;

import java.io.Serializable;
import java.util.Set;

import de.dagere.peass.dependency.analysis.data.TestCase;

public class RTSResult implements Serializable {
   private static final long serialVersionUID = 700041797958688300L;

   private final Set<TestCase> tests;
   private final boolean isRunning;
   private String commitOld;

   public RTSResult(final Set<TestCase> tests, final boolean isRunning) {
      this.tests = tests;
      this.isRunning = isRunning;
   }

   public Set<TestCase> getTests() {
      return tests;
   }
   
   public boolean isRunning() {
      return isRunning;
   }
   
   public String getVersionOld() {
      return commitOld;
   }

   public void setVersionOld(final String commitOld) {
      this.commitOld = commitOld;
   }

   @Override
   public String toString() {
      return isRunning + " " + tests.toString();
   }
}
