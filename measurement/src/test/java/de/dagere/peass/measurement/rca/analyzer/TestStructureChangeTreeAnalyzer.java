package de.dagere.peass.measurement.rca.analyzer;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.helper.TreeBuilder;
import de.dagere.peass.measurement.rca.helper.TreeBuilderBig;

public class TestStructureChangeTreeAnalyzer {
   @Test
   public void testEqualTree() {
      final TreeBuilder predecessorBuilder = new TreeBuilder();
      CallTreeNode root = predecessorBuilder.getRoot();
      CallTreeNode rootPredecessor = predecessorBuilder.getRoot();
      
      rootPredecessor.setOtherKiekerPattern(null);
      rootPredecessor.getChildren().get(0).setOtherKiekerPattern(null);
      rootPredecessor.getChildren().get(1).setOtherKiekerPattern(null);

      StructureChangeTreeAnalyzer analyzer = new StructureChangeTreeAnalyzer(root, rootPredecessor);

      Assert.assertEquals("public void Test.test()", rootPredecessor.getOtherKiekerPattern());
      Assert.assertEquals("public void ClassA.methodA()", rootPredecessor.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertEquals("public void ClassC.methodC()", rootPredecessor.getChildren().get(1).getOtherKiekerPattern());
      
      MatcherAssert.assertThat(analyzer.getMeasurementNodesPredecessor(), Matchers.hasItems(predecessorBuilder.getRoot(), predecessorBuilder.getA()));
   }

   @Test
   public void testAddedTree() {
      CallTreeNode root = new TreeBuilder().getRoot();
      final TreeBuilderBig bigBuilder = new TreeBuilderBig(true);
      CallTreeNode rootPredecessor = bigBuilder.getRoot();

      StructureChangeTreeAnalyzer analyzer = new StructureChangeTreeAnalyzer(root, rootPredecessor);

      MatcherAssert.assertThat(analyzer.getMeasurementNodesPredecessor(), Matchers.hasItems(bigBuilder.getRoot(), bigBuilder.getA()));
      MatcherAssert.assertThat(analyzer.getMeasurementNodesPredecessor(), Matchers.not(Matchers.hasItems(bigBuilder.getB2())));
   }
}
