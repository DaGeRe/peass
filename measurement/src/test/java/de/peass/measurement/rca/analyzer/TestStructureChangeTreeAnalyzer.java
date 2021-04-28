package de.peass.measurement.rca.analyzer;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.measurement.rca.analyzer.StructureChangeTreeAnalyzer;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.helper.TreeBuilder;
import de.peass.measurement.rca.helper.TreeBuilderBig;

public class TestStructureChangeTreeAnalyzer {
   @Test
   public void testEqualTree() {
      final TreeBuilder predecessorBuilder = new TreeBuilder();
      CallTreeNode root = predecessorBuilder.getRoot();
      CallTreeNode rootPredecessor = predecessorBuilder.getRoot();

      StructureChangeTreeAnalyzer analyzer = new StructureChangeTreeAnalyzer(root, rootPredecessor);

      Assert.assertThat(analyzer.getMeasurementNodesPredecessor(), Matchers.hasItems(predecessorBuilder.getRoot(), predecessorBuilder.getA()));
   }

   @Test
   public void testAddedTree() {
      CallTreeNode root = new TreeBuilder().getRoot();
      final TreeBuilderBig bigBuilder = new TreeBuilderBig(true);
      CallTreeNode rootPredecessor = bigBuilder.getRoot();

      StructureChangeTreeAnalyzer analyzer = new StructureChangeTreeAnalyzer(root, rootPredecessor);

      Assert.assertThat(analyzer.getMeasurementNodesPredecessor(), Matchers.hasItems(bigBuilder.getRoot(), bigBuilder.getA()));
      Assert.assertThat(analyzer.getMeasurementNodesPredecessor(), Matchers.not(Matchers.hasItems(bigBuilder.getB2())));
   }
}
