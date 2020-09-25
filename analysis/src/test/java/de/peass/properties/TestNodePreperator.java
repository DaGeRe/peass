package de.peass.properties;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.measurement.analysis.statistics.TestcaseStatistic;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.measurement.rca.serialization.MeasuredNode;
import de.peass.visualization.NodePreparator;

public class TestNodePreperator {
   
   private CauseSearchData data = new CauseSearchData();
   private final TestcaseStatistic statistic = new TestcaseStatistic(1, 2, 0.1, 0.1, 100, 3, true, 5, 0);
   
   @Before
   public void setUp() {
      data.setConfig(new MeasurementConfiguration(100, 100, 0.1, 0.1));
   }
   
   @Test
   public void testChildPreperation() {
      prepareTree();

      final NodePreparator preparator = new NodePreparator(data);
      preparator.prepare();

      Assert.assertEquals(2, preparator.getRootNode().getChildren().size());
      Assert.assertEquals(3, preparator.getRootNode().getChildren().get(0).getChildren().size());
   }

   @Test
   public void testSubtreePreperation() {
      prepareLongTree();
      
      final NodePreparator preparator = new NodePreparator(data);
      preparator.prepare();

      Assert.assertEquals(3, preparator.getRootNode().getChildren().size());
      Assert.assertEquals(2, preparator.getRootNode().getChildren().get(0).getChildren().size());
      Assert.assertEquals(2, preparator.getRootNode().getChildren().get(1).getChildren().size());
   }

   private void prepareLongTree() {
      final MeasuredNode root = new MeasuredNode("Test.testMethod", "public void Test.testMethod()");
      
      final MeasuredNode child1 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()");
      final MeasuredNode child2 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()");
      final MeasuredNode child3 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()");
      root.setChilds(Arrays.asList(new MeasuredNode[] { child1, child2, child3 }));
      
      final MeasuredNode child11 = new MeasuredNode("ClassA.method11", "public void ClassA.method11()");
      final MeasuredNode child12 = new MeasuredNode("ClassA.method12", "public void ClassA.method12()");
      child1.setChilds(Arrays.asList(new MeasuredNode[] {child11, child12}));
      
      final MeasuredNode child21 = new MeasuredNode("ClassA.method11", "public void ClassA.method11()");
      final MeasuredNode child22 = new MeasuredNode("ClassA.method12", "public void ClassA.method12()");
      child2.setChilds(Arrays.asList(new MeasuredNode[] {child21, child22}));
      
      final MeasuredNode child31 = new MeasuredNode("ClassA.method11", "public void ClassA.method11()");
      final MeasuredNode child32 = new MeasuredNode("ClassA.method13", "public void ClassA.method13()");
      child3.setChilds(Arrays.asList(new MeasuredNode[] {child31, child32}));
      
      setChildrenStatistic(statistic, root);
      
      data.setNodes(root);
   }

   private void prepareTree() {
      final TestcaseStatistic statistic = new TestcaseStatistic(1, 2, 0.1, 0.1, 100, 3, true, 5, 0);

      final MeasuredNode root = new MeasuredNode("Test.testMethod", "public void Test.testMethod()");

      final MeasuredNode child1 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()");
      final MeasuredNode child2 = new MeasuredNode("ClassA.method2", "public void ClassA.method2()");
      root.setChilds(Arrays.asList(new MeasuredNode[] { child1, child2 }));

      final MeasuredNode child11 = new MeasuredNode("ClassA.method11", "public void ClassA.method11()");
      final MeasuredNode child12 = new MeasuredNode("ClassA.method12", "public void ClassA.method12()");
      final MeasuredNode child13 = new MeasuredNode("ClassA.method13", "public void ClassA.method13()");
      child1.setChilds(Arrays.asList(new MeasuredNode[] { child11, child12, child13 }));

      setChildrenStatistic(statistic, root);
      
      data.setNodes(root);
   }
   
   private void setChildrenStatistic(final TestcaseStatistic statistic, final MeasuredNode parent) {
      parent.setStatistic(statistic);
      for (final MeasuredNode node : parent.getChildren()) {
         System.out.println("Setting: " + node);
         node.setStatistic(statistic);
         setChildrenStatistic(statistic, node);
      }
   }
}
