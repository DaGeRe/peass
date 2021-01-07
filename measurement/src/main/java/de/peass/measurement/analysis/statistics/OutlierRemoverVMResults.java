package de.peass.measurement.analysis.statistics;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import de.peass.measurement.rca.data.OneVMResult;

public class OutlierRemoverVMResults {
   public static void getValuesWithoutOutliers(List<OneVMResult> results, SummaryStatistics statistics) {
      SummaryStatistics fullStatistic = new SummaryStatistics();
      for (final OneVMResult result : results) {
         final double average = result.getAverage();
         fullStatistic.addValue(average);
      }
      
      double min = fullStatistic.getMean() - OutlierRemover.Z_SCORE * fullStatistic.getStandardDeviation();
      double max = fullStatistic.getMean() + OutlierRemover.Z_SCORE * fullStatistic.getStandardDeviation();
      
      for (final OneVMResult result : results) {
         final double average = result.getAverage();
         if (average >= min && average <= max) {
            statistics.addValue(average);
         }
      }
   }
}
