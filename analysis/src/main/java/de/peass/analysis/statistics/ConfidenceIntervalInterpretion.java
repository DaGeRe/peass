package de.peass.analysis.statistics;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.generated.Result;
import de.peass.measurement.analysis.Relation;
import de.peass.measurement.analysis.statistics.MeasurementAnalysationUtil;
import de.peass.statistics.ConfidenceInterval;
import de.peass.statistics.PerformanceChange;
import de.precision.analysis.repetitions.bimodal.CompareData;

public class ConfidenceIntervalInterpretion {

   private static final Logger LOG = LogManager.getLogger(ConfidenceIntervalInterpretion.class);

   public static DescriptiveStatistics getStatistics(final List<Result> results) {
      final DescriptiveStatistics ds = new DescriptiveStatistics();
      results.forEach(result -> ds.addValue(result.getValue()));
      return ds;
   }

   public static Relation compare(CompareData data) {
      return compare(data, 96);
   }

   /**
    * Returns the relation of the two empirical results, based on confidence interval interpretation, viewed from the _first_ result. E.g. LESS_THAN means that the first result is
    * LESS_THAN the second, and GREATER_THAN means the first result is GREATER_THAN the second.
    * 
    * @param before
    * @param after
    * @return
    */
   public static Relation compare(CompareData data, int percentage) {
      final ConfidenceInterval intervalBefore = getConfidenceInterval(data.getBefore(), percentage);
      final ConfidenceInterval intervalAfter = getConfidenceInterval(data.getAfter(), percentage);

      final double avgBefore = data.getAvgBefore();
      final double avgAfter = data.getAvgAfter();

      LOG.trace("Intervalle: {} ({}) vs. vorher {} ({})", intervalAfter, avgAfter, intervalBefore, avgBefore);
      final PerformanceChange change = new PerformanceChange(intervalBefore, intervalAfter, "", "", "0", "1");
      final double diff = change.getDifference();
      if (intervalBefore.getMax() < intervalAfter.getMin()) {
         // if (change.getNormedDifference() > MeasurementAnalysationUtil.MIN_NORMED_DISTANCE && diff > MeasurementAnalysationUtil.MIN_ABSOLUTE_PERCENTAGE_DISTANCE *
         // intervalAfter.getMax()) {
         LOG.trace("Änderung: {} {} Diff: {}", change.getRevisionOld(), change.getTestMethod(), diff);
         LOG.trace("Ist kleiner geworden: {} vs. vorher {}", intervalAfter, intervalBefore);
         LOG.trace("Abstand: {} Versionen: {}:{}", diff);
         return Relation.LESS_THAN;
         // }
      }
      if (intervalBefore.getMin() > intervalAfter.getMax()) {
         // if (change.getNormedDifference() > MeasurementAnalysationUtil.MIN_NORMED_DISTANCE && diff > MeasurementAnalysationUtil.MIN_ABSOLUTE_PERCENTAGE_DISTANCE *
         // intervalAfter.getMax()) {
         LOG.trace("Änderung: {} {} Diff: {}", change.getRevisionOld(), change.getTestMethod(), diff);
         LOG.trace("Ist größer geworden: {} vs. vorher {}", intervalAfter, intervalBefore);
         LOG.trace("Abstand: {}", diff);
         return Relation.GREATER_THAN;
         // }
      }

      return Relation.EQUAL;
   }

   public static ConfidenceInterval getConfidenceInterval(final double[] valuesBefore, int percentage) {
      // LOG.info(valuesBefore + " " + valuesBefore.length);
      final ConfidenceInterval intervalBefore = MeasurementAnalysationUtil.getBootstrapConfidenceInterval(valuesBefore, valuesBefore.length, 100, percentage);
      return intervalBefore;
   }
}
