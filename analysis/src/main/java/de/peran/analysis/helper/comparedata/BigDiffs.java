package de.peran.analysis.helper.comparedata;

import java.util.LinkedHashMap;
import java.util.Map;

import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.analysis.statistics.TestcaseStatistic;

public class BigDiffs {
   Map<String, TestcaseDiff> diffs = new LinkedHashMap<>();
   Map<String, DecissionCount> diffCount = new LinkedHashMap<>();

   public void addDifferingValue(final String version, final TestCase testCase, final String folder, final TestcaseStatistic statistic) {
      TestcaseDiff versionDiff = diffs.get(version);
      if (versionDiff == null) {
         versionDiff = new TestcaseDiff();
         diffs.put(version, versionDiff);
      }
      DifferentMeasurements measurements = versionDiff.getTestcases().get(testCase);
      if (measurements == null) {
         measurements = new DifferentMeasurements();
         versionDiff.getTestcases().put(testCase, measurements);
      }
      measurements.getMeasurements().put(folder, statistic);
   }
   
   public void addChange(String folder, String version, TestCase testcase, TestcaseStatistic statistic) {
      // TODO Auto-generated method stub
      
   }

   public void incrementDiff(String folder) {
      DecissionCount diff = getDiff(folder);
      diff.setBigDiff(diff.getBigDiff() + 1);
   }

   public void incrementTTestDiff(String folder) {
      DecissionCount diff = getDiff(folder);
      diff.settTestDiff(diff.gettTestDiff() + 1);
   }
   
   public void incrementMeasurements(String folder) {
      DecissionCount diff = getDiff(folder);
      diff.setMeasurements(diff.getMeasurements() + 1);
   }
   
   public void incrementConfidenceDiff(String folder) {
      DecissionCount diff = getDiff(folder);
      diff.setConfidenceDiff(diff.getConfidenceDiff() + 1);
   }

   private DecissionCount getDiff(String folder) {
      DecissionCount diff = diffCount.get(folder);
      if (diff == null) {
         diff = new DecissionCount();
         diffCount.put(folder, diff);
      }
      return diff;
   }

   public Map<String, DecissionCount> getDiffCount() {
      return diffCount;
   }

   public void setDiffCount(Map<String, DecissionCount> diffCount) {
      this.diffCount = diffCount;
   }

   public Map<String, TestcaseDiff> getDiffs() {
      return diffs;
   }

   public void setDiffs(final Map<String, TestcaseDiff> diffs) {
      this.diffs = diffs;
   }

   
}