package de.dagere.peass.measurement.rca.kiekerReading;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;

public class TestOperationExecutionReading {
   private static final File EXAMPLE_DATA_FOLDER = new File("src/test/resources/operationExecutionExample");

   private ChangedTreeBuilder builder;

   

   @Test
   public void testAdding() {
      builder = new ChangedTreeBuilder();
      CallTreeNode root = builder.getRoot();

      Set<CallTreeNode> includedNodes = createIncludedNodes(root);

      KiekerDurationReader.executeDurationStage(new File(EXAMPLE_DATA_FOLDER, "version0"), includedNodes, ChangedTreeBuilder.VERSION0);
      KiekerDurationReader.executeDurationStage(new File(EXAMPLE_DATA_FOLDER, "version1"), includedNodes, ChangedTreeBuilder.VERSION1);

      for (CallTreeNode node : includedNodes) {
         if (!node.getCall().equals(CauseSearchData.ADDED)) {
            node.createStatistics(ChangedTreeBuilder.VERSION0);
            SummaryStatistics nodeStatistic = node.getStatistics(ChangedTreeBuilder.VERSION0);
            Assert.assertEquals("Node " + node.getCall() + " did not have correct call count", 1, nodeStatistic.getN());
         }
      }
      Assert.assertEquals("Node " + root.getCall() + " did not have correct mean", 2, root.getStatistics(ChangedTreeBuilder.VERSION0).getMean(), 0.01);

      builder.getA1().createStatistics(ChangedTreeBuilder.VERSION1);
      Assert.assertEquals("Node " + builder.getA1().getOtherVersionNode().getCall() + " did not have correct call count", 1, builder.getA1().getStatistics(ChangedTreeBuilder.VERSION1).getN());
      Assert.assertEquals("Node " + builder.getA1().getOtherVersionNode().getCall() + " did not have correct mean", 10, builder.getA1().getStatistics(ChangedTreeBuilder.VERSION1).getMean(), 0.01);
   }

   private Set<CallTreeNode> createIncludedNodes(CallTreeNode root2) {
      Set<CallTreeNode> includedNodes = new HashSet<>();

      includedNodes.add(root2);
      includedNodes.addAll(root2.getChildren());
      includedNodes.addAll(builder.getA1().getChildren());

      for (CallTreeNode node : includedNodes) {
         node.initVersions();
      }
      return includedNodes;
   }

}
