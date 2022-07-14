package de.dagere.peass.dependency.traces.coverage;

import java.util.LinkedHashMap;
import java.util.Map;

import de.dagere.peass.dependency.analysis.data.TestCase;

public class CoverageSelectionCommit {
   public Map<TestCase, TraceCallSummary> testcases = new LinkedHashMap<>();

   public Map<TestCase, TraceCallSummary> getTestcases() {
      return testcases;
   }

   public void setTestcases(final Map<TestCase, TraceCallSummary> testcases) {
      this.testcases = testcases;
   }
}
