package de.dagere.peass.measurement.analysis;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.measurement.statistics.Relation;
import de.dagere.peass.measurement.statistics.StatisticUtil;

public class TestStatisticUtil {

   @Test
   public void testAlternativeT() {
      final double[] vals = new double[] { 1.1, 1.1, 1.2, 1.3, 1.4, 1.1, 1.2, 1.3, 1.4, 1.23, 1.1, 1.1, 1.2, 1.3, 1.4, 1.1, 1.2, 1.3, 1.4, 1.23 };
      final double[] vals2 = new double[] { 1.3, 1.1, 1.2, 1.5, 1.4, 1.2, 1.3, 1.5, 1.4, 1.4 };
      final double[] vals3 = new double[] { 0.9, 1.1, 1.2, 1.1, 0.8, };
      final double[] vals4 = new double[] { 1.9, 1.7, 1.6, 1.5, 1.8, 1.6, 1.3, 1.5, 1.8, 1.7, 1.9, 1.7, 1.6, 1.5, 1.8, 1.6, 1.3, 1.5, 1.8, 1.7 };

      final DescriptiveStatistics statistics1 = new DescriptiveStatistics(vals);
      final DescriptiveStatistics statistics2 = new DescriptiveStatistics(vals2);
      final DescriptiveStatistics statistics3 = new DescriptiveStatistics(vals3);
      final DescriptiveStatistics statistics4 = new DescriptiveStatistics(vals4);

      Assert.assertEquals(TestUtils.homoscedasticT(statistics1, statistics2), StatisticUtil.getTValue(statistics1, statistics2, 0.0), 0.01);
      Assert.assertEquals(TestUtils.homoscedasticT(statistics2, statistics3), StatisticUtil.getTValue(statistics2, statistics3, 0.0), 0.01);
      Assert.assertEquals(TestUtils.homoscedasticT(statistics1, statistics3), StatisticUtil.getTValue(statistics1, statistics3, 0.0), 0.01);

      // = assuming equal = true
      // Assert.assertTrue(StatisticUtil.differenceHypothesisGetsRejected(statistics1, statistics1, 0.001));
      // Assert.assertTrue(StatisticUtil.differenceHypothesisGetsRejected(statistics2, statistics2, 0.001));
      // Assert.assertTrue(StatisticUtil.differenceHypothesisGetsRejected(statistics3, statistics3, 0.001));

      System.out.println(statistics1.getMean() + " " + statistics1.getStandardDeviation() + " " + statistics4.getMean() + " " + statistics4.getStandardDeviation());

      // Assert.assertFalse(StatisticUtil.rejectAreDifferent(statistics1, statistics4, 0.001, 0.01));
      // Assert.assertFalse(StatisticUtil.rejectAreDifferent(statistics1, statistics4, 0.001, 0.1));
      // Assert.assertTrue(StatisticUtil.rejectAreDifferent(statistics1, statistics4, 0.001, 0.5));

      // Assert.assertFalse(StatisticUtil.rejectAreDifferent(statistics1, statistics4, 0.01, 0.1));
      // Assert.assertTrue(StatisticUtil.rejectAreDifferent(statistics1, statistics4, 0.15, 0.1));

      // Assert.assertTrue(StatisticUtil.rejectAreDifferent(statistics1, statistics3, 0.001, 0.01));

   }

   /**
    * Ziel: Abbruch wenn areEqual = true
    */
   @Test
   public void testRejectDifferent() {
      final double[] vals = new double[] { 1.0, 1.1, 1.2 };
      final double[] vals2 = new double[] { 0.9, 1.0, 1.0 };
      final double[] vals3 = new double[] { 1.7, 1.6, 1.5 };
      final double[] vals4 = new double[] { 1.1, 1.1, 1.0, };

      final DescriptiveStatistics statistics1 = new DescriptiveStatistics(vals);
      final DescriptiveStatistics statistics2 = new DescriptiveStatistics(vals2);
      final DescriptiveStatistics statistics3 = new DescriptiveStatistics(vals3);
      final DescriptiveStatistics statistics4 = new DescriptiveStatistics(vals4);

      Assert.assertEquals(Relation.UNKOWN, StatisticUtil.agnosticTTest(statistics1, statistics2, 0.05, 0.05));
      // Increased type-1-error should lead to unequal measurement (even if they are considered unequal with lower type-1-error)
      // -> increasing the type-1-error leads to a false positive (since type-1-error-rate is false-positive rate)
      Assert.assertEquals(Relation.GREATER_THAN, StatisticUtil.agnosticTTest(statistics1, statistics2, 0.25, 0.05));
      
      
      Assert.assertEquals(Relation.LESS_THAN, StatisticUtil.agnosticTTest(statistics1, statistics3, 0.05, 0.05));
      
      // High type-2-error should lead to equal measurement (false negative)
      Assert.assertEquals(Relation.EQUAL, StatisticUtil.agnosticTTest(statistics1, statistics4, 0.05, 0.40));
      Assert.assertEquals(Relation.EQUAL, StatisticUtil.agnosticTTest(statistics1, statistics1, 0.05, 0.05));
   }

}
