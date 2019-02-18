package de.peran.analysis.helper.comparedata;

import java.util.LinkedHashMap;
import java.util.Map;

import de.peass.dependency.analysis.data.TestCase;

public class TestcaseDiff {
   Map<TestCase, DifferentMeasurements> testcases = new LinkedHashMap<>();

   public Map<TestCase, DifferentMeasurements> getTestcases() {
      return testcases;
   }

   public void setTestcases(final Map<TestCase, DifferentMeasurements> testcases) {
      this.testcases = testcases;
   }
}