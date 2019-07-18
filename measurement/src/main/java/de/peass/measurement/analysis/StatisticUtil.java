package de.peass.measurement.analysis;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.experimental.theories.suppliers.TestedOn;

public class StatisticUtil {

   private static final Logger LOG = LogManager.getLogger(StatisticUtil.class);

   /**
    * Agnostic T-Test from Coscrato et al.: Agnostic tests can control the type I and type II errors simultaneously
    */
   public static Relation agnosticTTest(DescriptiveStatistics statisticsAfter, DescriptiveStatistics statisticsBefore, double type1error, double type2error) {
      final TDistribution tDistribution = new TDistribution(null, statisticsAfter.getN() + statisticsBefore.getN() - 2);
      final double criticalValueEqual = Math.abs(tDistribution.inverseCumulativeProbability(0.5 * (1 + type1error)));
      final double criticalValueUnequal = Math.abs(tDistribution.inverseCumulativeProbability(1. - 0.5 * type2error));

      LOG.info("Allowed errors: {} {}", type1error, type2error);
      LOG.info("Critical values: {} {}", criticalValueUnequal, criticalValueEqual);

      double tValue = getTValue(statisticsAfter, statisticsBefore, 0);
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
   public static boolean areEqual(DescriptiveStatistics statisticsAfter, DescriptiveStatistics statisticsBefore, double significance, double maxDelta) {
      final double delta_mu = statisticsAfter.getMean() - statisticsBefore.getMean();

      final TDistribution tDistribution = new TDistribution(null, statisticsAfter.getN() + statisticsBefore.getN() - 2);
      final double criticalValue = Math.abs(tDistribution.inverseCumulativeProbability(significance));

      System.out.println("tcrit: " + criticalValue);

      double tValue0 = getTValue(statisticsAfter, statisticsBefore, delta_mu);
      System.out.println("Delta: " + delta_mu + " Max: " + maxDelta * statisticsAfter.getMean() + " T0: " + tValue0);
      if (Math.abs(delta_mu) < maxDelta * statisticsAfter.getMean()) {
         return true;
      }
      double tested_delta = delta_mu;
      while (Math.abs(tested_delta) > maxDelta * statisticsAfter.getMean()) {
         double tValue = getTValue(statisticsAfter, statisticsBefore, tested_delta);
         System.out.println("Delta: " + tested_delta + " T:" + tValue + " Max: " + maxDelta * statisticsAfter.getMean());
         if (Math.abs(tValue) > criticalValue) {
            return false;
         }

         tested_delta = tested_delta / 2;
      }

      return true; // falsch -> Eigentlich unbekannt
   }

   public static boolean rejectAreEqual(DescriptiveStatistics statisticsAfter, DescriptiveStatistics statisticsBefore, double significance) {
      final TDistribution tDistribution = new TDistribution(null, statisticsAfter.getN() + statisticsBefore.getN() - 2);
      final double criticalValue = Math.abs(tDistribution.inverseCumulativeProbability(significance));

      double tValue0 = getTValue(statisticsAfter, statisticsBefore, 0.0);

      return Math.abs(tValue0) > criticalValue;
   }

   public static double getTValue(DescriptiveStatistics statisticsAfter, DescriptiveStatistics statisticsBefore, double omega) {
      double n = statisticsAfter.getN();
      double m = statisticsBefore.getN();
      double sizeFactor = Math.sqrt(m * n / (m + n));
      double upperPart = (m - 1) * Math.pow(statisticsBefore.getStandardDeviation(), 2) + (n - 1) * Math.pow(statisticsAfter.getStandardDeviation(), 2);
      double s = Math.sqrt(upperPart / (m + n - 2));
      double difference = (statisticsAfter.getMean() - statisticsBefore.getMean() - omega);
      double tAlternative = sizeFactor * difference / s;
      return tAlternative;
   }
}
