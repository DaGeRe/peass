package de.dagere.peass.measurement.rca.data;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.MeasurementConfig;

public class CallTreeNodeWarmupTest {

   @Test
   public void testWarmup() {
      List<StatisticalSummary> values = new LinkedList<>();
      values.add(new StatisticalSummaryValues(100, 10, 75, 100, 100, 75 * 100));
      values.add(new StatisticalSummaryValues(25, 5, 100, 25, 25, 100 * 25));

      MeasurementConfig config = new MeasurementConfig(5, "1", "1");
      config.setWarmup(25);

      final CallTreeNode node = initNode(values, config);
      Assert.assertEquals(50.0, node.getStatistics("1").getMean(), 0.01);
   }

   @Test
   public void testWarmupWithRepetitions() {
      List<StatisticalSummary> values = new LinkedList<>();
      values.add(new StatisticalSummaryValues(100, 10, 700, 100, 100, 70 * 100));
      values.add(new StatisticalSummaryValues(25, 5, 1000, 25, 25, 100 * 25));

      MeasurementConfig config = new MeasurementConfig(5, "1", "1");
      config.setWarmup(20);
      config.setRepetitions(10);
      config.setIterations(150);
      config.getKiekerConfig().setUseAggregation(false);
      final CallTreeNode node = initNode(values, config);
      Assert.assertEquals(50.0, node.getStatistics("1").getMean(), 0.01);
   }

   @Test
   public void testWarmupWithRepetitions2() {
      List<StatisticalSummary> values = new LinkedList<>();
      values.add(new StatisticalSummaryValues(100, 10, 100, 100, 100, 70 * 100));
      values.add(new StatisticalSummaryValues(25, 5, 100, 25, 25, 100 * 25));

      MeasurementConfig config = new MeasurementConfig(5, "1", "1");
      config.setWarmup(10);
      config.setRepetitions(10);
      config.setIterations(10);
      config.getKiekerConfig().setUseAggregation(false);
      final CallTreeNode node = initNode(values, config);
      Assert.assertEquals(25.0, node.getStatistics("1").getMean(), 0.01);
   }
   
   @Test
   public void testTooLessValues() {
      List<StatisticalSummary> values = new LinkedList<>();
      values.add(new StatisticalSummaryValues(100, 10, 4, 100, 100, 75 * 100));

      MeasurementConfig config = new MeasurementConfig(5, "1", "1");
      config.setWarmup(5);

      final CallTreeNode node = initNode(values, config);
      Assert.assertEquals(0.0, node.getStatistics("1").getN(), 0.01);
   }

   private CallTreeNode initNode(final List<StatisticalSummary> values, final MeasurementConfig config) {
      final CallTreeNode node = new CallTreeNode("de.mypackage.Test#callMethod",
            "public void de.mypackage.Test.callMethod()",
            "public void de.mypackage.Test.callMethod()",
            config);
      final CallTreeNode otherVersionNode = new CallTreeNode("de.mypackage.Test#callMethod",
            "public void de.mypackage.Test.callMethod()",
            "public void de.mypackage.Test.callMethod()",
            config);
      node.setOtherCommitNode(otherVersionNode);

      node.initCommitData();
      node.addAggregatedMeasurement("1", values);

      node.createStatistics("1");
      return node;
   }
}
