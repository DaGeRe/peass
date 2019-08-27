package de.peass.measurement.analysis;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.measurement.MeasurementConfiguration;

public class StatisticUtil {

   private static final Logger LOG = LogManager.getLogger(StatisticUtil.class);

   /**
    * Agnostic T-Test from Coscrato et al.: Agnostic tests can control the type I and type II errors simultaneously
    */
   public static Relation agnosticTTest(final StatisticalSummary statisticsPrev, final StatisticalSummary statisticsVersion, final double type1error, final double type2error) {
      final double tValue = getTValue(statisticsPrev, statisticsVersion, 0);
      return agnosticTTest(tValue, statisticsPrev.getN() + statisticsVersion.getN() - 2, type1error, type2error);
   }
   
   public static Relation agnosticTTest(final double tValue, final long degreesOfFreedom, final double type1error, final double type2error) {
      final TDistribution tDistribution = new TDistribution(null, degreesOfFreedom);
      final double criticalValueEqual = Math.abs(tDistribution.inverseCumulativeProbability(0.5 * (1 + type1error)));
      final double criticalValueUnequal = Math.abs(tDistribution.inverseCumulativeProbability(1. - 0.5 * type2error));

      LOG.info("Allowed errors: {} {}", type1error, type2error);
      LOG.info("Critical values: {} {}", criticalValueUnequal, criticalValueEqual);

      LOG.info("T: {}", tValue);

      if (Math.abs(tValue) > criticalValueUnequal) {
         return Relation.UNEQUAL;
      } else if (Math.abs(tValue) < criticalValueEqual) {
         return Relation.EQUAL;
      } else {
         return Relation.UNKOWN;
      }
   }

   /**
    * Tested by testing whether it can be rejected that they are different - this does not work
    */
   public static boolean areEqual(final DescriptiveStatistics statisticsPrev, final DescriptiveStatistics statisticsVersion, final double significance, final double maxDelta) {
      final double delta_mu = statisticsPrev.getMean() - statisticsVersion.getMean();

      final TDistribution tDistribution = new TDistribution(null, statisticsPrev.getN() + statisticsVersion.getN() - 2);
      final double criticalValue = Math.abs(tDistribution.inverseCumulativeProbability(significance));

      System.out.println("tcrit: " + criticalValue);

      final double tValue0 = getTValue(statisticsPrev, statisticsVersion, delta_mu);
      System.out.println("Delta: " + delta_mu + " Max: " + maxDelta * statisticsPrev.getMean() + " T0: " + tValue0);
      if (Math.abs(delta_mu) < maxDelta * statisticsPrev.getMean()) {
         return true;
      }
      double tested_delta = delta_mu;
      while (Math.abs(tested_delta) > maxDelta * statisticsPrev.getMean()) {
         final double tValue = getTValue(statisticsPrev, statisticsVersion, tested_delta);
         System.out.println("Delta: " + tested_delta + " T:" + tValue + " Max: " + maxDelta * statisticsPrev.getMean());
         if (Math.abs(tValue) > criticalValue) {
            return false;
         }

         tested_delta = tested_delta / 2;
      }

      return true; // falsch -> Eigentlich unbekannt
   }

   public static boolean rejectAreEqual(final StatisticalSummary statisticsAfter, final StatisticalSummary statisticsBefore, final double significance) {
      final TDistribution tDistribution = new TDistribution(null, statisticsAfter.getN() + statisticsBefore.getN() - 2);
      final double criticalValue = Math.abs(tDistribution.inverseCumulativeProbability(significance));

      final double tValue0 = getTValue(statisticsAfter, statisticsBefore, 0.0);

      return Math.abs(tValue0) > criticalValue;
   }

   public static double getTValue(final StatisticalSummary statisticsAfter, final StatisticalSummary statisticsBefore, final double omega) {
      final double n = statisticsAfter.getN();
      final double m = statisticsBefore.getN();
      final double sizeFactor = Math.sqrt(m * n / (m + n));
      final double upperPart = (m - 1) * Math.pow(statisticsBefore.getStandardDeviation(), 2) + (n - 1) * Math.pow(statisticsAfter.getStandardDeviation(), 2);
      final double s = Math.sqrt(upperPart / (m + n - 2));
      final double difference = (statisticsAfter.getMean() - statisticsBefore.getMean() - omega);
      final double tAlternative = sizeFactor * difference / s;
      return tAlternative;
   }

   /**
    * Determines whether there is a change, no change or no decission can be made. Usually, first the predecessor and than the current version are given.
    * @param statisticsPrev
    * @param statisticsVersion
    * @param measurementConfig
    * @return
    */
   public static Relation agnosticTTest(final StatisticalSummary statisticsPrev, final StatisticalSummary statisticsVersion, final MeasurementConfiguration measurementConfig) {
      return agnosticTTest(statisticsPrev, statisticsVersion, measurementConfig.getType1error(), measurementConfig.getType2error());
   }
}
