package de.peass.measurement.analysis;

import java.util.List;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.execution.MeasurementConfiguration;

public class StatisticUtil {

   private static final Logger LOG = LogManager.getLogger(StatisticUtil.class);

   public static double getMean(final List<StatisticalSummary> statistics) {
      final StatisticalSummaryValues vals = AggregateSummaryStatistics.aggregate(statistics);
      return vals.getMean();
   }
   
   /**
    * Agnostic T-Test from Coscrato et al.: Agnostic tests can control the type I and type II errors simultaneously
    */
   public static Relation agnosticTTest(final StatisticalSummary statisticsPrev, final StatisticalSummary statisticsVersion, final double type1error, final double type2error) {
      final double tValue = getTValue(statisticsPrev, statisticsVersion, 0);
      return agnosticTTest(tValue, statisticsPrev.getN() + statisticsVersion.getN() - 2, type1error, type2error);
   }

   public static Relation agnosticTTest(final double tValue, final long degreesOfFreedom, final double type1error, final double type2error) {
      final double criticalValueEqual = getCriticalValueType1(type1error, degreesOfFreedom);
      final double criticalValueUnequal = getCriticalValueType2(type2error, degreesOfFreedom);

      LOG.trace("Allowed errors: {} {}", type1error, type2error);
      LOG.trace("Critical values: {} {}", criticalValueUnequal, criticalValueEqual);

      LOG.trace("T: {}", tValue);

      if (Math.abs(tValue) > criticalValueUnequal) {
         return Relation.UNEQUAL;
      } else if (Math.abs(tValue) < criticalValueEqual) {
         return Relation.EQUAL;
      } else {
         return Relation.UNKOWN;
      }
   }

   public static double getCriticalValueType2(final double type2error, final long degreesOfFreedom) {
      final TDistribution tDistribution = new TDistribution(null, degreesOfFreedom);
      final double criticalValueUnequal = Math.abs(tDistribution.inverseCumulativeProbability(1. - 0.5 * type2error));
      return criticalValueUnequal;
   }

   public static double getCriticalValueType1(final double type1error, final long degreesOfFreedom) {
      final TDistribution tDistribution = new TDistribution(null, degreesOfFreedom);
      final double criticalValueEqual = Math.abs(tDistribution.inverseCumulativeProbability(0.5 * (1 + type1error)));
      return criticalValueEqual;
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
    * 
    * @param statisticsPrev
    * @param statisticsVersion
    * @param measurementConfig
    * @return
    */
   public static Relation agnosticTTest(final StatisticalSummary statisticsPrev, final StatisticalSummary statisticsVersion, final MeasurementConfiguration measurementConfig) {
      return agnosticTTest(statisticsPrev, statisticsVersion, measurementConfig.getType1error(), measurementConfig.getType2error());
   }

   public static Relation isChange(final StatisticalSummary statisticsPrev, final StatisticalSummary statisticsVersion, final MeasurementConfiguration measurementConfig) {
      final double maxVal = Math.max(statisticsPrev.getMean(), statisticsVersion.getMean());
      if (maxVal > 1) {
         return agnosticTTest(statisticsPrev, statisticsVersion, measurementConfig);
//      } else if (maxVal > 1) {
//         final Relation r1 = isConfidenceIntervalOverlap(statisticsPrev, statisticsVersion, measurementConfig.getType2error());
//         final Relation r2 = agnosticTTest(statisticsPrev, statisticsVersion, measurementConfig.getType1error() / 10, measurementConfig.getType2error() / 10);
//         final Relation result = (r1 == r2) ? r2 : Relation.UNKOWN;
//         return result;
      } else {
         Relation r1 = agnosticTTest(statisticsPrev, statisticsVersion, measurementConfig.getType1error() / 20, measurementConfig.getType2error() / 20);
         final double tValue = getTValue(statisticsPrev, statisticsVersion, 0);
         if (Math.abs(tValue) < 10) {
            r1 = Relation.UNKOWN;
         }
         final Relation r2 = isConfidenceIntervalOverlap(statisticsPrev, statisticsVersion, measurementConfig.getType2error());
         final Relation result = ((r1 == r2) && (r2 == Relation.UNEQUAL)) ? Relation.UNEQUAL : Relation.UNKOWN;
         return result;
      }
   }

   public static Relation isConfidenceIntervalOverlap(final StatisticalSummary statisticsPrev, final StatisticalSummary statisticsVersion, final double confidenceLevel) {
      final double confidenceSpreadPrev = getConfidenceSpread(statisticsPrev, confidenceLevel);
      final double confidenceSpreadVersion = getConfidenceSpread(statisticsVersion, confidenceLevel);
      final double lowerPrev = statisticsPrev.getMean() - confidenceSpreadPrev;
      final double upperVersion = statisticsVersion.getMean() + confidenceSpreadVersion;
      final double lowerVersion = statisticsVersion.getMean() - confidenceSpreadVersion;
      final double upperPrev = statisticsPrev.getMean() + confidenceSpreadPrev;
      if (statisticsPrev.getMean() > statisticsVersion.getMean()) {
         if (lowerPrev > upperVersion) {
            return Relation.UNEQUAL;
         } else {
            return Relation.EQUAL;
         }
      } else {

         if (lowerVersion > upperPrev) {
            return Relation.UNEQUAL;
         } else {
            return Relation.EQUAL;
         }
      }
   }

   public static double getConfidenceSpread(final StatisticalSummary statistics, final double confidenceLevel) {
      final double standarderror = Math.sqrt(statistics.getStandardDeviation() * statistics.getStandardDeviation() / statistics.getN());
      // final TDistribution tDistribution = new TDistribution(null, statistics.getN());
      final NormalDistribution tDistribution = new NormalDistribution();
      final double areaSize = (1 - confidenceLevel) / 2;
      final double criticalValue = Math.abs(tDistribution.inverseCumulativeProbability(areaSize));
      // System.out.println(standarderror + " " + criticalValue);
      return criticalValue * standarderror;
   }
}
