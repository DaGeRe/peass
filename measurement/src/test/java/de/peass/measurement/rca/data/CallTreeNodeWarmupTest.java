package de.peass.measurement.rca.data;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.junit.Assert;
import org.junit.jupiter.api.Test;


public class CallTreeNodeWarmupTest {
   
   @Test
   public void testWarmup() {
      final CallTreeNode node = new CallTreeNode("de.mypackage.Test#callMethod", "public void de.mypackage.Test.callMethod()", "public void de.mypackage.Test.callMethod()");
      final CallTreeNode otherVersionNode = new CallTreeNode("de.mypackage.Test#callMethod", "public void de.mypackage.Test.callMethod()", "public void de.mypackage.Test.callMethod()");
      node.setOtherVersionNode(otherVersionNode);
      
      List<StatisticalSummary> values = new LinkedList<>();
      values.add(new StatisticalSummaryValues(100, 10, 75, 100, 100, 75*100));
      values.add(new StatisticalSummaryValues(25, 5, 100, 25, 25, 100*25));
      
      node.setWarmup(25);
      node.setVersions("1", "1~1");
      node.addAggregatedMeasurement("1", values);
      
      node.createStatistics("1");
      Assert.assertEquals(50.0, node.getStatistics("1").getMean(), 0.01);
   }
}
