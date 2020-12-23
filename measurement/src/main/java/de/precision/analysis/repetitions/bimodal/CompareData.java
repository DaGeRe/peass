package de.precision.analysis.repetitions.bimodal;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import de.dagere.kopeme.generated.Result;

public final class CompareData {
   private final double[] before;
   private final double[] after;
   private final SummaryStatistics beforeStat = new SummaryStatistics();
   private final SummaryStatistics afterStat = new SummaryStatistics();

   public CompareData(double[] before, double[] after) {
      this.before = before;
      this.after = after;
      for (double beforeVal : before) {
         beforeStat.addValue(beforeVal);
      }
      for (double afterVal : after) {
         afterStat.addValue(afterVal);
      }
   }
   
   public CompareData(final List<Result> beforeShortened, final List<Result> afterShortened) {
      {
         before = new double[beforeShortened.size()];
         int index = 0;
         for (Result result : beforeShortened) {
            before[index] = result.getValue();
            getBeforeStat().addValue(before[index]);
            index++;
         }
      }

      {
         after = new double[afterShortened.size()];
         int index = 0;
         for (Result result : afterShortened) {
            after[index] = result.getValue();
            getAfterStat().addValue(after[index]);
            index++;
         }
      }
   }
   
   public double getAvgAfter() {
      return getAfterStat().getMean();
   }
   
   public double getAvgBefore() {
      return getBeforeStat().getMean();
   }

   public double[] getBefore() {
      return before;
   }

   public double[] getAfter() {
      return after;
   }

   public SummaryStatistics getBeforeStat() {
      return beforeStat;
   }

   public SummaryStatistics getAfterStat() {
      return afterStat;
   }
}