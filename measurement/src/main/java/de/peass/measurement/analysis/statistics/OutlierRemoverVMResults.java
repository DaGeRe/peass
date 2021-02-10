package de.peass.measurement.analysis.statistics;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.measurement.rca.data.OneVMResult;

public class OutlierRemoverVMResults {

   private static final Logger LOG = LogManager.getLogger(OutlierRemoverVMResults.class);

   public static void getValuesWithoutOutliers(final List<OneVMResult> results, final SummaryStatistics statistics) {
      if (results.size() > 3) {
         SummaryStatistics fullStatistic = new SummaryStatistics();
         addAll(results, fullStatistic);

         double min = fullStatistic.getMean() - OutlierRemover.Z_SCORE * fullStatistic.getStandardDeviation();
         double max = fullStatistic.getMean() + OutlierRemover.Z_SCORE * fullStatistic.getStandardDeviation();

         LOG.debug("Removing outliers between {} and {} - Old vm count: {}", min, max, results.size());
         addNonOutlier(results, statistics, min, max);
         LOG.debug("VM count after removal: {}", statistics.getN());
      } else {
         LOG.debug("Skipping outlier removal because of VM count below 2");
         addAll(results, statistics);
      }
   }

   private static void addNonOutlier(final List<OneVMResult> results, final SummaryStatistics statistics, double min, double max) {
      for (final OneVMResult result : results) {
         final double average = result.getAverage();
         if (average >= min && average <= max) {
            statistics.addValue(average);
            LOG.trace("Adding value: {}", average);
         } else {
            LOG.debug("Not adding outlier: {}", average);
         }
      }
   }

   private static void addAll(final List<OneVMResult> results, final SummaryStatistics statistics) {
      for (final OneVMResult result : results) {
         final double average = result.getAverage();
         statistics.addValue(average);
      }
   }
}
