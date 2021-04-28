package de.precision.analysis.repetitions.bimodal;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import de.dagere.kopeme.generated.Result;
import de.dagere.peass.measurement.rca.data.OneVMResult;

public final class CompareData {
   private final double[] before;
   private final double[] after;
   private final SummaryStatistics beforeStat;
   private final SummaryStatistics afterStat;

   public CompareData(final double[] before, final double[] after) {
      this.before = before;
      this.after = after;
      if (before != null) {
         beforeStat = new SummaryStatistics();
         for (double beforeVal : before) {
            beforeStat.addValue(beforeVal);
         }
      } else {
         beforeStat = null;
      }
      if (after != null) {
         afterStat = new SummaryStatistics();
         for (double afterVal : after) {
            afterStat.addValue(afterVal);
         }
      } else {
         afterStat = null;
      }
   }

   public CompareData(final List<Result> beforeShortened, final List<Result> afterShortened) {
      {
         beforeStat = new SummaryStatistics();
         before = new double[beforeShortened.size()];
         int index = 0;
         for (Result result : beforeShortened) {
            before[index] = result.getValue();
            getBeforeStat().addValue(before[index]);
            index++;
         }
      }

      {
         afterStat = new SummaryStatistics();
         after = new double[afterShortened.size()];
         int index = 0;
         for (Result result : afterShortened) {
            after[index] = result.getValue();
            getAfterStat().addValue(after[index]);
            index++;
         }
      }
   }

   /**
    * Creates a CompareData instance from Lists of OneVMResults. Can't be a constructor, since it is not possible to have constructors with the same erasure (i.e. List, List)
    */
   public static CompareData createCompareDataFromOneVMResults(final List<OneVMResult> beforeVals, final List<OneVMResult> afterVals) {
      final double[] before = getDoubleArray(beforeVals);
      final double[] after = getDoubleArray(afterVals);

      return new CompareData(before, after);
   }

   private static double[] getDoubleArray(final List<OneVMResult> sourceVals) {
      final double[] valueArray;
      if (sourceVals != null) {
         valueArray = new double[sourceVals.size()];
         {
            int index = 0;
            for (OneVMResult result : sourceVals) {
               valueArray[index] = result.getAverage();
               index++;
            }
         }
      } else {
         valueArray = null;
      }
      return valueArray;
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