package de.dagere.peass.measurement.rca.data;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

import de.dagere.peass.statistics.StatisticUtil;


/**
 * Call tree result built of already analyzed data - for AggregatedTreeReader
 * 
 */
public class FinalCallTreeResult implements OneVMResult {
   private final List<StatisticalSummary> statistics;

   public FinalCallTreeResult(final List<StatisticalSummary> statistics) {
      this.statistics = statistics;
   }

   public double getAverage() {
      return StatisticUtil.getMean(statistics);
   }

   @Override
   public long getCalls() {
      long calls = 0;
      for (final StatisticalSummary summary : statistics) {
         calls += summary.getN();
      }
      return calls;
   }

   @Override
   public List<StatisticalSummary> getValues() {
      return statistics;
   }
}