package de.peass.analysis.helper.read;

import java.util.LinkedHashMap;
import java.util.Map;

import de.dagere.peass.dependency.analysis.data.TestCase;

public class TestcaseData {
   Map<TestCase, FolderValues> testcaseData = new LinkedHashMap<>();

   public Map<TestCase, FolderValues> getTestcaseData() {
      return testcaseData;
   }

   public void setTestcaseData(final Map<TestCase, FolderValues> testcaseData) {
      this.testcaseData = testcaseData;
   }
}