package de.dagere.peass.measurement.statistics;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.measurement.statistics.bimodal.CompareData;

public class ConfidenceIntervalInterpretion {

   private static final Logger LOG = LogManager.getLogger(ConfidenceIntervalInterpretion.class);

   public static DescriptiveStatistics getStatistics(final List<VMResult> results) {
      final DescriptiveStatistics ds = new DescriptiveStatistics();
      results.forEach(result -> ds.addValue(result.getValue()));
      return ds;
   }

   public static Relation compare(final CompareData data) {
      return compare(data, 96);
   }

   public static Relation compare(final CompareData data, final double type1error) {
      int confidenceIntervalPercentage = (int) (100 - type1error * 100);
      return compare(data, confidenceIntervalPercentage);
   }

   /**
    * Returns the relation of the two empirical results, based on confidence interval interpretation, viewed from the _first_ result. E.g. LESS_THAN means that the first result is
    * LESS_THAN the second, and GREATER_THAN means the first result is GREATER_THAN the second.
    * 
    * @param before
    * @param after
    * @return
    */
   public static Relation compare(final CompareData data, final int percentage) {
      if (percentage < 1 || percentage > 99) {
         throw new RuntimeException("Percentage between 1 and 99 expected");
      }
      final ConfidenceInterval intervalPredecessor = getConfidenceInterval(data.getPredecessor(), percentage);
      final ConfidenceInterval intervalCurrent = getConfidenceInterval(data.getCurrent(), percentage);

      final double avgPredecessor = data.getAvgPredecessor();
      final double avgCurrent = data.getAvgCurrent();

      LOG.trace("Intervalle: {} ({}) vs. vorher {} ({})", intervalCurrent, avgCurrent, intervalPredecessor, avgPredecessor);
      final PerformanceChange change = new PerformanceChange(intervalPredecessor, intervalCurrent, "", "", "0", "1");
      final double diff = change.getDifference();
      if (intervalPredecessor.getMax() < intervalCurrent.getMin()) {
         // if (change.getNormedDifference() > MeasurementAnalysationUtil.MIN_NORMED_DISTANCE && diff > MeasurementAnalysationUtil.MIN_ABSOLUTE_PERCENTAGE_DISTANCE *
         // intervalAfter.getMax()) {
         LOG.trace("Änderung: {} {} Diff: {}", change.getRevisionOld(), change.getTestMethod(), diff);
         LOG.trace("Ist kleiner geworden: {} vs. vorher {}", intervalCurrent, intervalPredecessor);
         LOG.trace("Abstand: {} Versionen: {}:{}", diff);
         return Relation.LESS_THAN;
         // }
      }
      if (intervalPredecessor.getMin() > intervalCurrent.getMax()) {
         // if (change.getNormedDifference() > MeasurementAnalysationUtil.MIN_NORMED_DISTANCE && diff > MeasurementAnalysationUtil.MIN_ABSOLUTE_PERCENTAGE_DISTANCE *
         // intervalAfter.getMax()) {
         LOG.trace("Änderung: {} {} Diff: {}", change.getRevisionOld(), change.getTestMethod(), diff);
         LOG.trace("Ist größer geworden: {} vs. vorher {}", intervalCurrent, intervalPredecessor);
         LOG.trace("Abstand: {}", diff);
         return Relation.GREATER_THAN;
         // }
      }

      return Relation.EQUAL;
   }
   
   private static final int BOOTSTRAP_SIZE = 100;
   private static ThreadLocal<double[]> threadLocalValues = new ThreadLocal<>();

   public static ConfidenceInterval getConfidenceInterval(final double[] rawValues, final int percentage) {
      // LOG.info(valuesBefore + " " + valuesBefore.length);
      
      double[] values = threadLocalValues.get();
      if (values == null) {
         values = new double[BOOTSTRAP_SIZE];
         threadLocalValues.set(values);
      }
      final ConfidenceInterval interval = MeasurementAnalysationUtil.getBootstrapConfidenceInterval(rawValues, rawValues.length, values, percentage);
      return interval;
   }
}
