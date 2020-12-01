package de.peass.measurement.rca.analyzer;

import java.util.LinkedList;
import java.util.List;

import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.treeanalysis.TreeUtil;

/**
 * Identifies the nodes of two trees which can be mapped without a change (i.e. same name, position and parent for every node)
 * 
 * @author reichelt
 *
 */
public class StructureChangeTreeAnalyzer {

   private final List<CallTreeNode> equalStructureNodes = new LinkedList<>();

   public StructureChangeTreeAnalyzer(final CallTreeNode root, final CallTreeNode rootPredecessor) {
      root.setOtherVersionNode(rootPredecessor);
      rootPredecessor.setOtherVersionNode(root);

      if (root.getKiekerPattern().equals(rootPredecessor.getKiekerPattern())) {
         equalStructureNodes.add(rootPredecessor);
         mapAllNodes(root, rootPredecessor);
      }
   }

   private void mapAllNodes(final CallTreeNode current, final CallTreeNode currentPredecessor) {
      TreeUtil.findChildMapping(current, currentPredecessor);

      for (CallTreeNode currentChild : current.getChildren()) {
         CallTreeNode childPredecessor = currentChild.getOtherVersionNode();
         if (currentChild.getKiekerPattern().equals(childPredecessor.getKiekerPattern())) {
            equalStructureNodes.add(childPredecessor);
            mapAllNodes(currentChild, childPredecessor);
         }
      }
   }
   
   public List<CallTreeNode> getEqualStructureNodes() {
      return equalStructureNodes;
   }
}
