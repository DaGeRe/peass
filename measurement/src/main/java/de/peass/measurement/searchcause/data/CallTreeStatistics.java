package de.peass.measurement.searchcause.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class CallTreeStatistics {
   
   private final int warmup;
   private final List<OneVMResult> results = new ArrayList<>();
   protected final SummaryStatistics statistics = new SummaryStatistics();

   public CallTreeStatistics(final int warmup) {
      this.warmup = warmup;
   }
   
   public void addMeasurement(final Long duration) {
      final CallTreeResult current = (CallTreeResult) results.get(results.size() - 1);
      current.addValue(duration);
   }
   
   public void setMeasurement(final StatisticalSummary statistic) {
      results.add(new FinalCallTreeResult(statistic));
   }
   
   public void newResult() {
      results.add(new CallTreeResult(warmup));
   }

   public void createStatistics() {
      statistics.clear();
      for (final OneVMResult result : results) {
         final double average = result.getAverage();
         statistics.addValue(average);
      }
   }
   
   public List<OneVMResult> getResults() {
      return results;
   }

   public SummaryStatistics getStatistics() {
      return statistics;
   }

   public void resetResults() {
      results.clear();
   }
}