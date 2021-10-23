package de.dagere.peass.measurement.rca.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import de.dagere.peass.config.StatisticsConfig;
import de.dagere.peass.measurement.analysis.statistics.OutlierRemoverVMResults;

public class CallTreeStatistics {
   
   private final int warmup;
   private final List<OneVMResult> results = new ArrayList<>();
   protected final SummaryStatistics statistics = new SummaryStatistics();

   public CallTreeStatistics(final int warmup) {
      this.warmup = warmup;
   }
   
   /***
    * Adds *one* measurement to the current (last) VM of the node - suitable for measurements per iteration. If aggregated data are obtained, 
    * rather use addAggregatedMeasurement
    * @param duration   duration of one iteration (usually in microseconds)
    */
   public void addMeasurement(final Long duration) {
      final CallTreeResult current = (CallTreeResult) results.get(results.size() - 1);
      current.addValue(duration);
   }
   
   /**
    * Adds measurement values of *one VM run*. All warmed-up measurement values of the VM should be removed.
    * @param statistic  list of all warmed-up aggregated measurement statistics of one VM
    */
   public void addAggregatedMeasurement(final List<StatisticalSummary> statistic) {
      results.add(new FinalCallTreeResult(statistic));
   }
   
   public void newResult() {
      results.add(new CallTreeResult(warmup));
   }

   public void createStatistics(final StatisticsConfig config) {
      statistics.clear();
      OutlierRemoverVMResults.getValuesWithoutOutliers(results, statistics, config);
   }
   
   public List<OneVMResult> getResults() {
      return results;
   }

   /**
    * Returns the statistics *after* warmup consideration and outlier removal - values may therefore be unequal to full values from csv-measuremets
    * @return statistics of VM averages
    */
   public SummaryStatistics getStatistics() {
      return statistics;
   }

   public void resetResults() {
      results.clear();
   }

   public long getCalls() {
      return results.stream().mapToLong(result -> result.getCalls()).sum();
   }
}