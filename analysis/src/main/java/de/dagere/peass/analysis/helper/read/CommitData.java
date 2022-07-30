package de.dagere.peass.analysis.helper.read;

import java.util.LinkedHashMap;
import java.util.Map;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;

public class CommitData {
   private final Map<String, TestcaseData> data = new LinkedHashMap<>();

   public Map<String, TestcaseData> getData() {
      return data;
   }

   public void addStatistic(final String commit, final TestCase testcase, final String fileName,
         final TestcaseStatistic stat1, final boolean isTChange, final boolean isConfidenceChange) {
      TestcaseData testcaseData = data.get(commit);
      if (testcaseData == null) {
         testcaseData = new TestcaseData();
         data.put(commit, testcaseData);
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