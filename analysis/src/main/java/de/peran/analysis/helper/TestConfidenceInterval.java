package de.peran.analysis.helper;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TTest;

import de.peass.statistics.ConfidenceInterval;
import de.peass.statistics.MeasurementAnalysationUtil;

public class TestConfidenceInterval {
   public static void main(final String[] args) {
      final NormalDistribution distribution = new NormalDistribution(1.0, 0.25);
      final NormalDistribution distribution2 = new NormalDistribution(1.25, 0.25);

      for (int experiment = 0; experiment < 1000; experiment++) {
         int changesFalsePositive = 0;
         int changesReal = 0;
         int confFalsePositive = 0;
         int confReal = 0;
         final int ITERATIONS = 1000;
         final DescriptiveStatistics breakStatistic = new DescriptiveStatistics();
         for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            final int MAX = 30;
            final double[] vals = new double[MAX];
            final double[] vals2 = new double[MAX];
            final double[] vals3 = new double[MAX];
            int earlybreakCount = 0;
            for (int i = 0; i < MAX; i++) {
               vals[i] = distribution.sample();
               vals2[i] = distribution.sample();
               vals3[i] = distribution2.sample();
               if (i > 3) {
                  final DescriptiveStatistics stat = new DescriptiveStatistics(vals);
                  final DescriptiveStatistics stat2 = new DescriptiveStatistics(vals2);
                  final DescriptiveStatistics stat3 = new DescriptiveStatistics(vals3);
                  if (stat.getStandardDeviation() / stat.getMean() < 0.02 &&
                        stat2.getStandardDeviation() / stat3.getMean() < 0.02 &&
                        stat3.getStandardDeviation() / stat3.getMean() < 0.02) {
                     earlybreakCount = i;
                     i = MAX;
                     breakStatistic.addValue(earlybreakCount);
                  }
                  if (i > 3) {
                     // double[] test = new double[i + 2];
                     // System.arraycopy(vals3, 0, test, 0, i);
                     // final ConfidenceInterval interval3 = MeasurementAnalysationUtil.getBootstrapConfidenceInterval(test, MAX / 20, 1000, 96);
                     // double mean = interval3.getMin() + (interval3.getMax() - interval3.getMin()) / 2;
                     // System.out.println(i + " " + interval3.getLength() / mean);
                  }
               }
            }

            final DescriptiveStatistics stat = new DescriptiveStatistics(vals);
            final DescriptiveStatistics stat2 = new DescriptiveStatistics(vals2);
            final DescriptiveStatistics stat3 = new DescriptiveStatistics(vals3);

            final int confCount = MAX;
            final ConfidenceInterval interval1 = MeasurementAnalysationUtil.getBootstrapConfidenceInterval(vals, confCount, 1000, 96);
            final ConfidenceInterval interval2 = MeasurementAnalysationUtil.getBootstrapConfidenceInterval(vals2, confCount, 1000, 96);
            final ConfidenceInterval interval3 = MeasurementAnalysationUtil.getBootstrapConfidenceInterval(vals3, confCount, 1000, 96);

            if (compare(interval1, interval2)) {
               confFalsePositive++;
               System.out.println("FP: " + interval1 + " vs " + interval2 + " Means: " + new TTest().tTest(stat, stat2, 0.02));
               // System.out.println(Arrays.toString(vals) + " " + interval1);
               // System.out.println(Arrays.toString(vals2) + " " + interval2);
            }

            if (compare(interval1, interval3)) {
               confReal++;
            } else {
               System.out.println("FN: " + interval1 + " vs " + interval3);
            }

            final boolean tTest = new TTest().tTest(stat, stat2, 0.02);
            // System.out.println(new TTest().t(stat, stat2) + " " + tTest);
            if (tTest) {
               changesFalsePositive++;
            }
            // System.out.println(new TTest().t(stat, stat3) + " " + new TTest().tTest(stat, stat3, 0.02));
            if (new TTest().tTest(stat, stat3, 0.02)) {
               changesReal++;
            }
         }
         System.out.println("T-Changes (FP): " + changesFalsePositive + " T-Changes (TP): " + changesReal + " ConfChanges (FP): " + confFalsePositive + " ConfChanges (TP): "
               + confReal + " Early Break: " + breakStatistic.getMean());
      }

   }

   public static boolean compare(final ConfidenceInterval intervalBefore, final ConfidenceInterval intervalAfter) {
      // final PerformanceChange change = new PerformanceChange(intervalBefore, intervalAfter, "", "", "0", "1");
      // final double diff = change.getDifference();
      // System.out.println(intervalAfter + " After: " + intervalBefore);
      if (intervalBefore.getMax() < intervalAfter.getMin()) {
         // if (change.getNormedDifference() > MeasurementAnalysationUtil.MIN_NORMED_DISTANCE && diff > MeasurementAnalysationUtil.MIN_ABSOLUTE_PERCENTAGE_DISTANCE *
         // intervalAfter.getMax()) {
         return true;
         // }
      }
      if (intervalBefore.getMin() > intervalAfter.getMax()) {
         // if (change.getNormedDifference() > MeasurementAnalysationUtil.MIN_NORMED_DISTANCE && diff > MeasurementAnalysationUtil.MIN_ABSOLUTE_PERCENTAGE_DISTANCE *
         // intervalAfter.getMax()) {
         return true;
         // }
      }
      return false;
   }
}
