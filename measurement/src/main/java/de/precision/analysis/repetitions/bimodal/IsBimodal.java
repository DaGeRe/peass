package de.precision.analysis.repetitions.bimodal;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import de.dagere.kopeme.generated.Result;

public class IsBimodal {
   private final SummaryStatistics stat1 = new SummaryStatistics();
   private final SummaryStatistics stat2 = new SummaryStatistics();
   
   private final double avgValue;

   private final boolean isBimodal;

   public IsBimodal(double[] values, double avgValue, SummaryStatistics originalStat) {
      this.avgValue = avgValue;
      for (double value : values) {
         if (value < avgValue) {
            stat1.addValue(value);
         } else {
            stat2.addValue(value);
         }
      }

      isBimodal = testBimodal(originalStat);
   }

   public IsBimodal(List<Result> fastShortened) {
      SummaryStatistics stat = new SummaryStatistics();
      for (Result result : fastShortened) {
         stat.addValue(result.getValue());

      }
      avgValue = stat.getMean();
      for (Result result : fastShortened) {
         if (result.getValue() < avgValue) {
            stat1.addValue(result.getValue());
         } else {
            stat2.addValue(result.getValue());
         }
      }
      
      isBimodal = testBimodal(stat);
   }
   
   private boolean testBimodal(SummaryStatistics originalStat) {
      return stat1.getVariance() + stat2.getVariance() < originalStat.getVariance() / 4
            && stat1.getN() > 2 && stat2.getN() > 2;
   }
   
   public double getAvgValue() {
      return avgValue;
   }

   public SummaryStatistics getStat1() {
      return stat1;
   }

   public SummaryStatistics getStat2() {
      return stat2;
   }

   public boolean isBimodal() {
      return isBimodal;
   }

}
