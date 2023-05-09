package de.dagere.peass.measurement.rca.treeanalysis;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.helper.TestConstants;
import de.dagere.peass.measurement.rca.helper.TreeBuilder;

public class TestLevelDifferentNodeDeterminer {
   @Test
   public void testLevelDifferingDeterminer() {
      final TreeBuilder predecessorBuilder = new TreeBuilder();
      predecessorBuilder.addDE();
      final CallTreeNode rootPredecessor = predecessorBuilder.getRoot();
      final CallTreeNode root = new TreeBuilder().getRoot();
      rootPredecessor.setOtherCommitNode(root);
      root.setOtherCommitNode(rootPredecessor);

      predecessorBuilder.buildMeasurements(rootPredecessor);

      final LevelDifferentNodeDeterminer determiner = getDiff(rootPredecessor, root);
      Assert.assertEquals(1, determiner.getLevelDifferentCurrent().size());
      
      getDiff(rootPredecessor.getChildren(), root.getChildren());

      final LevelDifferentNodeDeterminer determinerD = getDiff(predecessorBuilder.getB(), predecessorBuilder.getB());
      Assert.assertEquals(1, determinerD.getLevelDifferentPredecessor().size());
   }

   private LevelDifferentNodeDeterminer getDiff(final CallTreeNode rootOld, final CallTreeNode rootMain) {
      final List<CallTreeNode> currentPredecessorNodeList = Arrays.asList(new CallTreeNode[] { rootOld });
      final List<CallTreeNode> currentVersionNodeList = Arrays.asList(new CallTreeNode[] { rootMain });
      return getDiff(currentPredecessorNodeList, currentVersionNodeList);
   }

   private LevelDifferentNodeDeterminer getDiff(final List<CallTreeNode> currentPredecessorNodeList, final List<CallTreeNode> currentVersionNodeList) {
      final LevelDifferentNodeDeterminer determiner = new LevelDifferentNodeDeterminer(currentPredecessorNodeList, currentVersionNodeList,
            TestConstants.SIMPLE_CAUSE_CONFIG, TestConstants.SIMPLE_MEASUREMENT_CONFIG);
      determiner.calculateDiffering();
      return determiner;
   }

   @Test
   public void testLevelDifferentNodeDeterminerIncludingF() {
      final TreeBuilder currentBuilder = new TreeBuilder();
      currentBuilder.addDE();

      final TreeBuilder predecessorBuilder = new TreeBuilder();
      predecessorBuilder.addDE2();
      predecessorBuilder.addF();
      final CallTreeNode rootPredecessor = predecessorBuilder.getRoot();
      final CallTreeNode root = currentBuilder.getRoot();
      rootPredecessor.setOtherCommitNode(root);
      root.setOtherCommitNode(rootPredecessor);
      
      predecessorBuilder.buildMeasurements(rootPredecessor);

      final LevelDifferentNodeDeterminer determiner = getDiff(rootPredecessor, root);
      Assert.assertEquals(1, determiner.getLevelDifferentCurrent().size());

      final LevelDifferentNodeDeterminer determinerD = getDiff(rootPredecessor.getChildren(), Arrays.asList(new CallTreeNode[] { currentBuilder.getC(), currentBuilder.getA() }));
      // Assert.assertEquals(1, determinerD.getLevelDifferentPredecessor().size());

      printDebugOutput(predecessorBuilder);
      
      Assert.assertEquals("public void ClassD.methodD()", predecessorBuilder.getD().getOtherKiekerPattern());
      Assert.assertEquals("public void ClassE.methodE()", predecessorBuilder.getE().getOtherKiekerPattern());
      
      Assert.assertEquals("public void ClassB.methodB()", predecessorBuilder.getB().getOtherKiekerPattern());
      Assert.assertEquals(CauseSearchData.ADDED, predecessorBuilder.getF().getOtherKiekerPattern());
   }

   private void printDebugOutput(final TreeBuilder predecessorBuilder) {
      System.out.println("C");
      for (CallTreeNode childOfC : predecessorBuilder.getC().getChildren()) {
         System.out.println(childOfC.getKiekerPattern() + " " + childOfC.getOtherCommitNode());
      }
      System.out.println();

      System.out.println("A");
      for (CallTreeNode childOfA : predecessorBuilder.getA().getChildren()) {
         System.out.println(childOfA.getKiekerPattern() + " " + childOfA.getOtherCommitNode());
      }
   }
}
