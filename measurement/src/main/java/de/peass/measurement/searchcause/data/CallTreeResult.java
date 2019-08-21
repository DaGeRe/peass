package de.peass.measurement.searchcause.data;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

class CallTreeResult {

   private final int warmup;
   private int measured = 0;
   private final SummaryStatistics statistics = new SummaryStatistics();

   public CallTreeResult(final int warmup) {
      this.warmup = warmup;
   }
   
   public double getAverage(){
      return statistics.getMean();
   }

   public void addValue(final Long value) {
      measured++;
      if (measured > warmup) {
         statistics.addValue(value);
      }
   }
}