package de.dagere.peass.analysis.helper.read;

import java.util.LinkedHashMap;
import java.util.Map;

import de.dagere.nodeDiffGenerator.data.TestMethodCall;

public class TestcaseData {
   Map<TestMethodCall, FolderValues> testcaseData = new LinkedHashMap<>();

   public Map<TestMethodCall, FolderValues> getTestcaseData() {
      return testcaseData;
   }

   public void setTestcaseData(final Map<TestMethodCall, FolderValues> testcaseData) {
      this.testcaseData = testcaseData;
   }
}