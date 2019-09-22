package de.peass.measurement.searchcause.data;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;

/**
 * Call tree result built of already analyzed data - for AggregatedTreeReader
 * 
 */
public class FinalCallTreeResult implements OneVMResult {
   private final StatisticalSummary statistics;

   public FinalCallTreeResult(final StatisticalSummary statistics) {
      super();
      this.statistics = statistics;
   }

   public double getAverage() {
      return statistics.getMean();
   }
}