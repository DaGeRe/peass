package de.precision.analysis.repetitions.bimodal;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.codehaus.groovy.runtime.ArrayUtil;

import de.dagere.kopeme.generated.Result;
import de.peass.measurement.analysis.MultipleVMTestUtil;
import de.peass.measurement.analysis.statistics.OutlierRemover;

public class OutlierRemoverBimodal {
   public OutlierRemoverBimodal(final List<Result> results) {
      final IsBimodal isBimodal = new IsBimodal(results);
      if (isBimodal.isBimodal()) {
         removeFromLeftDistribution(results, isBimodal);
         removeFromRightDistribution(results, isBimodal);
      } else {
         removeUnimodal(results);
      }
   }

   private void removeUnimodal(final List<Result> results) {
      SummaryStatistics statistics = MultipleVMTestUtil.getStatistic(results);
      for (Iterator<Result> iterator = results.iterator(); iterator.hasNext();) {
         Result r = iterator.next();
         double zscore = Math.abs(r.getValue() - statistics.getMean()) / statistics.getStandardDeviation();
         if (zscore > OutlierRemover.Z_SCORE) {
            iterator.remove();
         }
      }
   }

   private void removeFromRightDistribution(final List<Result> fastShortened, final IsBimodal isBimodal) {
      SummaryStatistics stat2 = isBimodal.getStat2();
      for (Iterator<Result> iterator = fastShortened.iterator(); iterator.hasNext();) {
         Result r = iterator.next();
         if (r.getValue() > isBimodal.getAvgValue()) {
            double zscore = Math.abs(r.getValue() - stat2.getMean()) / stat2.getStandardDeviation();
            if (zscore > OutlierRemover.Z_SCORE) {
               iterator.remove();
            }
         }
      }
   }

   private void removeFromLeftDistribution(final List<Result> fastShortened, final IsBimodal isBimodal) {
      SummaryStatistics stat1 = isBimodal.getStat1();
      for (Iterator<Result> iterator = fastShortened.iterator(); iterator.hasNext();) {
         Result r = iterator.next();
         if (r.getValue() < isBimodal.getAvgValue()) {
            double zscore = Math.abs(r.getValue() - stat1.getMean()) / stat1.getStandardDeviation();
            if (zscore > OutlierRemover.Z_SCORE) {
               iterator.remove();
            }
         }
      }
   }

   public static CompareData removeOutliers(CompareData data) {
      CompareData result;
      final BimodalityTester isBismodal = new BimodalityTester(data);
      if (isBismodal.isBimodal()) {
         double[] valuesBefore = removeOutliersBimodal(data.getBefore(), isBismodal.getDataBefore());
         double[] valuesAfter = removeOutliersBimodal(data.getAfter(), isBismodal.getDataAfter());
         result = new CompareData(valuesBefore, valuesAfter);
      } else {
         double[] valuesBefore = removeOutliers(data.getBefore(), data.getBeforeStat());
         double[] valuesAfter = removeOutliers(data.getAfter(), data.getAfterStat());
         result = new CompareData(valuesBefore, valuesAfter);
      }
      return result;
   }

   private static double[] removeOutliersBimodal(double[] values, final IsBimodal beforeData) {
      List<Double> containedValues = new LinkedList<>();
      for (double value : values) {
         if (value < beforeData.getAvgValue()) {
            double zscore = Math.abs(value - beforeData.getStat1().getMean()) / beforeData.getStat1().getStandardDeviation();
            if (!(zscore > OutlierRemover.Z_SCORE)) {
               containedValues.add(value);
            }
         } else {
            double zscore = Math.abs(value - beforeData.getStat2().getMean()) / beforeData.getStat2().getStandardDeviation();
            if (!(zscore > OutlierRemover.Z_SCORE)) {
               containedValues.add(value);
            }
         }
      }
      double[] containedResults = ArrayUtils.toPrimitive(containedValues.toArray(new Double[0]));
      return containedResults;
   }

   private static double[] removeOutliers(double[] original, StatisticalSummary statistics) {
      List<Double> containedValues = new LinkedList<>();
      for (double value : original) {
         double zscore = Math.abs(value - statistics.getMean()) / statistics.getStandardDeviation();
         if (!(zscore > OutlierRemover.Z_SCORE)) {
            containedValues.add(value);
         }
      }
      double[] containedResults = ArrayUtils.toPrimitive(containedValues.toArray(new Double[0]));
      return containedResults;
   }
}
