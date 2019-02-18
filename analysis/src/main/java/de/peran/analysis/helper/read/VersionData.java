package de.peran.analysis.helper.read;

import java.util.LinkedHashMap;
import java.util.Map;

import de.peass.dependency.analysis.data.TestCase;
import de.peran.measurement.analysis.Statistic;

public class VersionData {
   private Map<String, TestcaseData> data = new LinkedHashMap<>();

   public Map<String, TestcaseData> getData() {
      return data;
   }

   public void setData(Map<String, TestcaseData> data) {
      this.data = data;
   }

   public void addStatistic(final String version, final TestCase testcase, final String fileName,
         final Statistic stat1, boolean isTChange, boolean isConfidenceChange) {
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