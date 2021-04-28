package de.dagere.peass.measurement.rca.data;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 * CallTreeResult for analyzing a log line by line
 * 
 */
public class CallTreeResult implements OneVMResult {

   private final int warmup;
   private int measured = 0;
   private final SummaryStatistics statistics = new SummaryStatistics();

   public CallTreeResult(final int warmup) {
      this.warmup = warmup;
   }

   public double getAverage() {
      return statistics.getMean();
   }

   public void addValue(final Long value) {
      measured++;
      if (measured > warmup) {
         statistics.addValue(value);
      }
   }

   @Override
   public long getCalls() {
      return measured;
   }

   @Override
   public List<StatisticalSummary> getValues() {
      final List<StatisticalSummary> values = new LinkedList<>();
      values.add(statistics);
      return values;
   }
}