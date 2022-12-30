package de.dagere.peass.visualization;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.serialization.MeasuredNode;
import de.dagere.peass.measurement.rca.serialization.MeasuredValues;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;

public class TestNodePreperatorColor {

   private CauseSearchData data = new CauseSearchData();
   private final TestcaseStatistic statistic = new TestcaseStatistic(1, 2, 0.1, 0.1, 100, 3, 0.001, true, 5, 3);
   private final MeasuredNode root = new MeasuredNode("Test.testMethod", "public void Test.testMethod()", "public void Test.testMethod()");

   @BeforeEach
   public void setUp() {
      MeasurementConfig config = new MeasurementConfig(100);
      config.getExecutionConfig().setTimeout(100);
      data.setConfig(config);
      data.setNodes(root);
      root.setValues(new MeasuredValues());
      root.setValuesPredecessor(new MeasuredValues());
   }

   @Test
   public void testSourceChange() {
      final MeasuredNode child1 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()", "public void ClassA.method1()");
      final MeasuredNode child2 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()", CauseSearchData.ADDED);
      final MeasuredNode child3 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()", "public void ClassA.methodX()");
      root.setChilds(Arrays.asList(new MeasuredNode[] { child1, child2, child3 }));
      for (MeasuredNode node : root.getChilds()) {
         node.setValues(new MeasuredValues());
         node.setValuesPredecessor(new MeasuredValues());
      }

      setChildrenStatistic(statistic, root);

      final NodePreparator preparator = new NodePreparator(data);
      preparator.prepare();

      Assert.assertFalse(preparator.getGraphRoot().isHasSourceChange());
      Assert.assertFalse(preparator.getGraphRoot().getChildren().get(0).isHasSourceChange());
      Assert.assertTrue(preparator.getGraphRoot().getChildren().get(1).isHasSourceChange());
      Assert.assertTrue(preparator.getGraphRoot().getChildren().get(2).isHasSourceChange());
   }

   @Test
   public void testChildPreperation() {
      prepareTree();

      final NodePreparator preparator = new NodePreparator(data);
      preparator.prepare();

      Assert.assertEquals(2, preparator.getGraphRoot().getChildren().size());
      Assert.assertEquals(3, preparator.getGraphRoot().getChildren().get(0).getChildren().size());
   }

   @Test
   public void testSubtreePreperation() {
      prepareLongTree();

      final NodePreparator preparator = new NodePreparator(data);
      preparator.prepare();

      Assert.assertEquals(3, preparator.getGraphRoot().getChildren().size());
      Assert.assertEquals(2, preparator.getGraphRoot().getChildren().get(0).getChildren().size());
      Assert.assertEquals(2, preparator.getGraphRoot().getChildren().get(1).getChildren().size());
   }

   private void prepareLongTree() {
      final MeasuredNode child1 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()", "public void ClassA.method1()");
      final MeasuredNode child2 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()", "public void ClassA.method1()");
      final MeasuredNode child3 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()", "public void ClassA.method1()");
      root.setChilds(Arrays.asList(new MeasuredNode[] { child1, child2, child3 }));

      final MeasuredNode child11 = new MeasuredNode("ClassA.method11", "public void ClassA.method11()", "public void ClassA.method11()");
      final MeasuredNode child12 = new MeasuredNode("ClassA.method12", "public void ClassA.method12()", "public void ClassA.method12()");
      child1.setChilds(Arrays.asList(new MeasuredNode[] { child11, child12 }));

      final MeasuredNode child21 = new MeasuredNode("ClassA.method11", "public void ClassA.method11()", "public void ClassA.method11()");
      final MeasuredNode child22 = new MeasuredNode("ClassA.method12", "public void ClassA.method12()", "public void ClassA.method12()");
      child2.setChilds(Arrays.asList(new MeasuredNode[] { child21, child22 }));

      final MeasuredNode child31 = new MeasuredNode("ClassA.method11", "public void ClassA.method11()", "public void ClassA.method11()");
      final MeasuredNode child32 = new MeasuredNode("ClassA.method13", "public void ClassA.method13()", "public void ClassA.method13()");
      child3.setChilds(Arrays.asList(new MeasuredNode[] { child31, child32 }));

      setChildrenStatistic(statistic, root);
      
      for (MeasuredNode node : Arrays.asList(new MeasuredNode[] { child1, child2, child3, child11, child11, child12, child21, child22, child31, child32 })) {
         node.setValues(new MeasuredValues());
         node.setValuesPredecessor(new MeasuredValues());
      }
   }

   private void prepareTree() {
      final TestcaseStatistic statistic = new TestcaseStatistic(1, 2, 0.1, 0.1, 100, 3, 0.001, true, 3, 5);

      final MeasuredNode child1 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()", "public void ClassA.method1()");
      final MeasuredNode child2 = new MeasuredNode("ClassA.method2", "public void ClassA.method2()", "public void ClassA.method2()");
      root.setChilds(Arrays.asList(new MeasuredNode[] { child1, child2 }));

      final MeasuredNode child11 = new MeasuredNode("ClassA.method11", "public void ClassA.method11()", "public void ClassA.method11()");
      final MeasuredNode child12 = new MeasuredNode("ClassA.method12", "public void ClassA.method12()", "public void ClassA.method12()");
      final MeasuredNode child13 = new MeasuredNode("ClassA.method13", "public void ClassA.method13()", "public void ClassA.method13()");
      child1.setChilds(Arrays.asList(new MeasuredNode[] { child11, child12, child13 }));

      setChildrenStatistic(statistic, root);

      for (MeasuredNode node : Arrays.asList(new MeasuredNode[] { child1, child2, child11, child12, child13 })) {
         node.setValues(new MeasuredValues());
         node.setValuesPredecessor(new MeasuredValues());
      }
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
