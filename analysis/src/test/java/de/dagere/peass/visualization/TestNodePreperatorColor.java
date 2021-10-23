package de.dagere.peass.visualization;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.analysis.statistics.TestcaseStatistic;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.serialization.MeasuredNode;

public class TestNodePreperatorColor {
   
   private CauseSearchData data = new CauseSearchData();
   private final TestcaseStatistic statistic = new TestcaseStatistic(1, 2, 0.1, 0.1, 100, 3, true, 5, 3);
   private final MeasuredNode root = new MeasuredNode("Test.testMethod", "public void Test.testMethod()", "public void Test.testMethod()");
   
   @BeforeEach
   public void setUp() {
      MeasurementConfig config = new MeasurementConfig(100);
      config.getExecutionConfig().setTimeout(100);
      data.setConfig(config);
      data.setNodes(root);
   }
   
   @Test
   public void testSourceChange() {
      final MeasuredNode child1 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()", "public void ClassA.method1()");
      final MeasuredNode child2 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()", CauseSearchData.ADDED);
      final MeasuredNode child3 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()", "public void ClassA.methodX()");
      root.setChilds(Arrays.asList(new MeasuredNode[] { child1, child2, child3 }));
      
      setChildrenStatistic(statistic, root);
      
      final NodePreparator preparator = new NodePreparator(data);
      preparator.prepare();
      
      Assert.assertFalse(preparator.getRootNode().isHasSourceChange());
      Assert.assertFalse(preparator.getRootNode().getChildren().get(0).isHasSourceChange());
      Assert.assertTrue(preparator.getRootNode().getChildren().get(1).isHasSourceChange());
      Assert.assertTrue(preparator.getRootNode().getChildren().get(2).isHasSourceChange());
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
      final MeasuredNode child1 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()", "public void ClassA.method1()");
      final MeasuredNode child2 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()", "public void ClassA.method1()");
      final MeasuredNode child3 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()", "public void ClassA.method1()");
      root.setChilds(Arrays.asList(new MeasuredNode[] { child1, child2, child3 }));
      
      final MeasuredNode child11 = new MeasuredNode("ClassA.method11", "public void ClassA.method11()", "public void ClassA.method11()");
      final MeasuredNode child12 = new MeasuredNode("ClassA.method12", "public void ClassA.method12()", "public void ClassA.method12()");
      child1.setChilds(Arrays.asList(new MeasuredNode[] {child11, child12}));
      
      final MeasuredNode child21 = new MeasuredNode("ClassA.method11", "public void ClassA.method11()", "public void ClassA.method11()");
      final MeasuredNode child22 = new MeasuredNode("ClassA.method12", "public void ClassA.method12()", "public void ClassA.method12()");
      child2.setChilds(Arrays.asList(new MeasuredNode[] {child21, child22}));
      
      final MeasuredNode child31 = new MeasuredNode("ClassA.method11", "public void ClassA.method11()", "public void ClassA.method11()");
      final MeasuredNode child32 = new MeasuredNode("ClassA.method13", "public void ClassA.method13()", "public void ClassA.method13()");
      child3.setChilds(Arrays.asList(new MeasuredNode[] {child31, child32}));
      
      setChildrenStatistic(statistic, root);
   }

   private void prepareTree() {
      final TestcaseStatistic statistic = new TestcaseStatistic(1, 2, 0.1, 0.1, 100, 3, true, 3, 5);

      final MeasuredNode child1 = new MeasuredNode("ClassA.method1", "public void ClassA.method1()", "public void ClassA.method1()");
      final MeasuredNode child2 = new MeasuredNode("ClassA.method2", "public void ClassA.method2()", "public void ClassA.method2()");
      root.setChilds(Arrays.asList(new MeasuredNode[] { child1, child2 }));

      final MeasuredNode child11 = new MeasuredNode("ClassA.method11", "public void ClassA.method11()", "public void ClassA.method11()");
      final MeasuredNode child12 = new MeasuredNode("ClassA.method12", "public void ClassA.method12()", "public void ClassA.method12()");
      final MeasuredNode child13 = new MeasuredNode("ClassA.method13", "public void ClassA.method13()", "public void ClassA.method13()");
      child1.setChilds(Arrays.asList(new MeasuredNode[] { child11, child12, child13 }));

      setChildrenStatistic(statistic, root);
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
