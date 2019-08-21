package de.peass.measurement.searchcause;

import de.peass.dependency.analysis.data.TestCase;

public class CauseSearcherConfig {
   private final String version;
   private final String predecessor;
   private final TestCase testCase;

   public CauseSearcherConfig(String version, String predecessor, TestCase testCase) {
      this.version = version;
      this.predecessor = predecessor;
      this.testCase = testCase;
   }
   
   public String getVersion() {
      return version;
   }

   public String getPredecessor() {
      return predecessor;
   }

   public TestCase getTestCase() {
      return testCase;
   }
}