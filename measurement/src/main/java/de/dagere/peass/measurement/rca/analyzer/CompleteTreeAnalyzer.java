package de.dagere.peass.measurement.rca.analyzer;

import java.util.LinkedList;
import java.util.List;

import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.treeanalysis.TreeUtil;

public class CompleteTreeAnalyzer implements TreeAnalyzer {
   private final List<CallTreeNode> treeStructureDiffering = new LinkedList<>();
   private final List<CallTreeNode> allNodesPredecessor = new LinkedList<>();

   public CompleteTreeAnalyzer(final CallTreeNode root, final CallTreeNode rootPredecessor) {
      root.setOtherKiekerPattern(rootPredecessor.getKiekerPattern());
      rootPredecessor.setOtherCommitNode(root);
      rootPredecessor.setOtherKiekerPattern(root.getKiekerPattern());
      
      
      allNodesPredecessor.add(rootPredecessor);

      mapAllNodes(root, rootPredecessor);
   }

   private void mapAllNodes(final CallTreeNode current, final CallTreeNode currentPredecessor) {
      TreeUtil.findChildMapping(current, currentPredecessor);
      findTreeStructureDiffering(current, currentPredecessor);

      for (CallTreeNode currentChild : current.getChildren()) {
         CallTreeNode predecessorChild = currentChild.getOtherCommitNode();
         mapAllNodes(currentChild, predecessorChild);

         allNodesPredecessor.add(predecessorChild);
      }
   }

   private void findTreeStructureDiffering(final CallTreeNode current, final CallTreeNode currentPredecessor) {
      current.getChildren().forEach(child -> {
         if (child.getCall().equals(CauseSearchData.ADDED)) {
            treeStructureDiffering.add(child.getOtherCommitNode());
         }
      });
      currentPredecessor.getChildren().forEach(child -> {
         if (child.getCall().equals(CauseSearchData.ADDED)) {
            treeStructureDiffering.add(child.getOtherCommitNode());
         }
      });
   }

   public List<CallTreeNode> getTreeStructureDiffering() {
      return treeStructureDiffering;
   }

   public List<CallTreeNode> getMeasurementNodesPredecessor() {
      return allNodesPredecessor;
   }
}