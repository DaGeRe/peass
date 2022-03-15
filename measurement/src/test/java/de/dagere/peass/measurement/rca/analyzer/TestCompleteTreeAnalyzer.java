package de.dagere.peass.measurement.rca.analyzer;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.hamcrest.core.IsSame;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.helper.TreeBuilder;
import de.dagere.peass.measurement.rca.helper.TreeBuilderBig;

public class TestCompleteTreeAnalyzer {

   @Test
   public void testEqualTree() {
      final TreeBuilder versionBuilder = new TreeBuilder();
      CallTreeNode root = versionBuilder.getRoot();
      CallTreeNode rootPredecessor = new TreeBuilder().getRoot();

      CompleteTreeAnalyzer analyzer = new CompleteTreeAnalyzer(root, rootPredecessor);

      MatcherAssert.assertThat(analyzer.getTreeStructureDiffering(), Matchers.emptyCollectionOf(CallTreeNode.class));
      MatcherAssert.assertThat(analyzer.getMeasurementNodesPredecessor(), Matchers.hasItems(versionBuilder.getRoot(), versionBuilder.getA()));
      
      MatcherAssert.assertThat(analyzer.getMeasurementNodesPredecessor(), Matchers.not(
            IsIterableContaining.hasItem(
            IsSame.sameInstance(versionBuilder.getA()))));
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
      
      Assert.assertNotNull(bigBuilder.getB2().getOtherKiekerPattern());
   }
}
