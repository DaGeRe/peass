package de.dagere.peass.measurement.rca.kiekerReading;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;

public class TestOperationExecutionReading {
   private static final File EXAMPLE_DATA_FOLDER = new File("src/test/resources/operationExecutionExample");

   private static final String VERSION0 = "000000";
   private static final String VERSION1 = "000001";

   public CallTreeNode buildTree() {
      MeasurementConfig config = new MeasurementConfig(2);
      config.getExecutionConfig().setVersion(VERSION1);
      config.getExecutionConfig().setVersionOld(VERSION0);
      CallTreeNode root = new CallTreeNode("de.dagere.peass.Test#test", "public void de.dagere.peass.Test.test()", "public void de.dagere.peass.Test.test()",
            config);
      CallTreeNode otherRoot = new CallTreeNode("de.dagere.peass.Test#test", "public void de.dagere.peass.Test.test()", "public void de.dagere.peass.Test.test()",
            config);
      root.setOtherVersionNode(otherRoot);
      otherRoot.setOtherVersionNode(root);

      CallTreeNode A1 = root.appendChild("de.dagere.peass.ClazzA#method", "public void de.dagere.peass.ClazzA.method()", "public void de.dagere.peass.ClazzA.method()");
      CallTreeNode A2 = otherRoot.appendChild("de.dagere.peass.ClazzA#method", "public void de.dagere.peass.ClazzA.method()", "public void de.dagere.peass.ClazzA.method()");
      A1.setOtherVersionNode(A2);
      A2.setOtherVersionNode(A1);

      CallTreeNode B1 = root.appendChild("de.dagere.peass.ClazzB#method", "public void de.dagere.peass.ClazzB.method()", "ADDED");
      CallTreeNode B2 = otherRoot.appendChild("ADDED", "ADDED", "public void de.dagere.peass.ClazzB.method()");
      B1.setOtherVersionNode(B2);
      B2.setOtherVersionNode(B1);

      return root;
   }

   @Test
   public void testAdding() {
      CallTreeNode root = buildTree();

      Set<CallTreeNode> includedNodes = createIncludedNodes(root);
      Set<CallTreeNode> includedNodesOther = createIncludedNodes(root.getOtherVersionNode());

      KiekerDurationReader.executeDurationStage(new File(EXAMPLE_DATA_FOLDER, "version0"), includedNodes, VERSION0);
      KiekerDurationReader.executeDurationStage(new File(EXAMPLE_DATA_FOLDER, "version1"), includedNodes, VERSION1);
      // KiekerDurationReader.executeDurationStage(new File("src/test/resources/operationExecutionExample"), includedNodesOther, VERSION1);

      for (CallTreeNode node : includedNodes) {
         System.out.println(node.getCall());
         node.createStatistics(VERSION0);
         Assert.assertEquals("Node " + node.getCall() + " did not have correct call count", 1, node.getStatistics(VERSION0).getN());
      }
   }

   private Set<CallTreeNode> createIncludedNodes(CallTreeNode root2) {
      Set<CallTreeNode> includedNodes = new HashSet<>();

      includedNodes.add(root2);
      includedNodes.addAll(root2.getChildren());

      for (CallTreeNode node : includedNodes) {
         node.initVersions();
      }
      return includedNodes;
   }

}
