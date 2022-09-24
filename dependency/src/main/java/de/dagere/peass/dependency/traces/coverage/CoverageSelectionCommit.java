package de.dagere.peass.dependency.traces.coverage;

import java.util.LinkedHashMap;
import java.util.Map;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;

public class CoverageSelectionCommit {
   public Map<TestMethodCall, TraceCallSummary> testcases = new LinkedHashMap<>();

   public Map<TestMethodCall, TraceCallSummary> getTestcases() {
      return testcases;
   }

   public void setTestcases(final Map<TestMethodCall, TraceCallSummary> testcases) {
      this.testcases = testcases;
   }
}
