package de.peass.measurement.searchcause.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class CallTreeStatistics {
   
   private final int warmup;
   private final List<CallTreeResult> results = new ArrayList<>();
   private final SummaryStatistics statistics = new SummaryStatistics();

   public CallTreeStatistics(final int warmup) {
      this.warmup = warmup;
   }
   
   public void addMeasurement(final Long duration) {
      final CallTreeResult current = results.get(results.size() - 1);
      current.addValue(duration);
   }

   public void newResult() {
      results.add(new CallTreeResult(warmup));
   }

   public void createStatistics() {
      statistics.clear();
      for (final CallTreeResult result : results) {
         final double average = result.getAverage();
         statistics.addValue(average);
      }
//      results.clear();
   }

   public SummaryStatistics getStatistics() {
      return statistics;
   }

   public void resetResults() {
      results.clear();
   }
}