package de.dagere.peass.measurement.statistics.bimodal;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.measurement.rca.data.OneVMResult;

public final class CompareData {
   private final double[] predecessor;
   private final double[] current;
   private final SummaryStatistics predecessorStat;
   private final SummaryStatistics currentStat;

   public CompareData(final double[] predecessor, final double[] current) {
      this.predecessor = predecessor;
      this.current = current;
      if (predecessor != null) {
         predecessorStat = new SummaryStatistics();
         for (double predecessorVal : predecessor) {
            predecessorStat.addValue(predecessorVal);
         }
      } else {
         predecessorStat = null;
      }
      if (current != null) {
         currentStat = new SummaryStatistics();
         for (double currentVal : current) {
            currentStat.addValue(currentVal);
         }
      } else {
         currentStat = null;
      }
   }

   public CompareData(final List<VMResult> predecessorShortened, final List<VMResult> currentShortened) {
      {
         predecessorStat = new SummaryStatistics();
         predecessor = new double[predecessorShortened.size()];
         int index = 0;
         for (VMResult result : predecessorShortened) {
            predecessor[index] = result.getValue();
            getPredecessorStat().addValue(predecessor[index]);
            index++;
         }
      }

      {
         currentStat = new SummaryStatistics();
         current = new double[currentShortened.size()];
         int index = 0;
         for (VMResult result : currentShortened) {
            current[index] = result.getValue();
            getCurrentStat().addValue(current[index]);
            index++;
         }
      }
   }

   /**
    * Creates a CompareData instance from Lists of OneVMResults. Can't be a constructor, since it is not possible to have constructors with the same erasure (i.e. List, List)
    */
   public static CompareData createCompareDataFromOneVMResults(final List<OneVMResult> predecessorVals, final List<OneVMResult> currentVals) {
      final double[] before = getDoubleArray(predecessorVals);
      final double[] after = getDoubleArray(currentVals);

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
      return getCurrentStat().getMean();
   }

   public double getAvgBefore() {
      return getPredecessorStat().getMean();
   }

   public double[] getPredecessor() {
      return predecessor;
   }

   public double[] getCurrent() {
      return current;
   }

   public SummaryStatistics getPredecessorStat() {
      return predecessorStat;
   }

   public SummaryStatistics getCurrentStat() {
      return currentStat;
   }
}