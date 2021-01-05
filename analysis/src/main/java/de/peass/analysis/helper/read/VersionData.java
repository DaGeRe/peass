package de.peass.analysis.helper.read;

import java.util.LinkedHashMap;
import java.util.Map;

import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.analysis.statistics.TestcaseStatistic;

public class VersionData {
   private final Map<String, TestcaseData> data = new LinkedHashMap<>();

   public Map<String, TestcaseData> getData() {
      return data;
   }

   public void addStatistic(final String version, final TestCase testcase, final String fileName,
         final TestcaseStatistic stat1, final boolean isTChange, final boolean isConfidenceChange) {
      TestcaseData testcaseData = data.get(version);
      if (testcaseData == null) {
         testcaseData = new TestcaseData();
         data.put(version, testcaseData);
      }
      FolderValues values = testcaseData.testcaseData.get(testcase);
      if (values == null) {
         values = new FolderValues();
         testcaseData.testcaseData.put(testcase, values);
      }
      values.getValues().put(fileName, stat1);
      values.getIsTChange().put(fileName, isTChange);
      values.getIsConfidenceChange().put(fileName, isConfidenceChange);
   }
}