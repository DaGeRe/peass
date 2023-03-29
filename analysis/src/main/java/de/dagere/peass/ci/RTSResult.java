package de.dagere.peass.ci;

import java.io.Serializable;
import java.util.Set;

import de.dagere.nodeDiffDetector.data.TestMethodCall;

public class RTSResult implements Serializable {
   private static final long serialVersionUID = 700041797958688300L;

   private final Set<TestMethodCall> tests;
   private final boolean isRunning;
   private String commitOld;

   public RTSResult(final Set<TestMethodCall> tests, final boolean isRunning) {
      this.tests = tests;
      this.isRunning = isRunning;
   }

   public Set<TestMethodCall> getTests() {
      return tests;
   }
   
   public boolean isRunning() {
      return isRunning;
   }
   
   public String getCommitOld() {
      return commitOld;
   }

   public void setCommitOld(final String commitOld) {
      this.commitOld = commitOld;
   }

   @Override
   public String toString() {
      return isRunning + " " + tests.toString();
   }
}
