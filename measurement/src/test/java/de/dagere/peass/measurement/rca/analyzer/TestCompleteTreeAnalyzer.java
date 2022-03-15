package de.dagere.peass.measurement.rca.analyzer;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.helper.TreeBuilder;
import de.dagere.peass.measurement.rca.helper.TreeBuilderBig;

public class TestCompleteTreeAnalyzer {

   @Test
   public void testEqualTree() {
      final TreeBuilder predecessorBuilder = new TreeBuilder();
      CallTreeNode root = predecessorBuilder.getRoot();
      CallTreeNode rootPredecessor = predecessorBuilder.getRoot();

      CompleteTreeAnalyzer analyzer = new CompleteTreeAnalyzer(root, rootPredecessor);

      MatcherAssert.assertThat(analyzer.getTreeStructureDiffering(), Matchers.emptyCollectionOf(CallTreeNode.class));
      MatcherAssert.assertThat(analyzer.getMeasurementNodesPredecessor(), Matchers.hasItems(predecessorBuilder.getRoot(), predecessorBuilder.getA()));
   }

   @Test
   public void testAddedTree() {
      CallTreeNode root = new TreeBuilder().getRoot();
      final TreeBuilderBig bigBuilder = new TreeBuilderBig(true);
      CallTreeNode rootPredecessor = bigBuilder.getRoot();

      CompleteTreeAnalyzer analyzer = new CompleteTreeAnalyzer(root, rootPredecessor);

      MatcherAssert.assertThat(analyzer.getTreeStructureDiffering(), Matchers.hasItem(bigBuilder.getB2()));
      MatcherAssert.assertThat(analyzer.getMeasurementNodesPredecessor(), Matchers.hasItems(bigBuilder.getRoot(), bigBuilder.getA()));
   }

   @Test
   public void testRemovedTree() {
      CallTreeNode rootPredecessor = new TreeBuilder().getRoot();
      final TreeBuilderBig bigBuilder = new TreeBuilderBig(true);
      CallTreeNode root = bigBuilder.getRoot();

      CompleteTreeAnalyzer analyzer = new CompleteTreeAnalyzer(root, rootPredecessor);

      MatcherAssert.assertThat(analyzer.getTreeStructureDiffering(), Matchers.hasItem(bigBuilder.getB2()));
      MatcherAssert.assertThat(analyzer.getMeasurementNodesPredecessor(), Matchers.hasItems(bigBuilder.getRoot(), bigBuilder.getA()));
   }
}
