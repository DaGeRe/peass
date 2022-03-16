package de.dagere.peass.measurement.rca.analyzer;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections4.BidiMap;

import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.treeanalysis.TreeUtil;

public class CompleteTreeAnalyzer implements TreeAnalyzer {
   private final List<CallTreeNode> treeStructureDiffering = new LinkedList<>();
   private final List<CallTreeNode> allNodesPredecessor = new LinkedList<>();

   public CompleteTreeAnalyzer(final CallTreeNode root, final CallTreeNode rootPredecessor) {
      root.setOtherKiekerPattern(rootPredecessor.getKiekerPattern());
      
      allNodesPredecessor.add(rootPredecessor);

      mapAllNodes(root, rootPredecessor);
   }

   private void mapAllNodes(final CallTreeNode current, final CallTreeNode currentPredecessor) {
      final BidiMap<CallTreeNode, CallTreeNode> mapping = TreeUtil.findChildMapping(current, currentPredecessor);
      findTreeStructureDiffering(current, currentPredecessor, mapping);

      for (CallTreeNode currentChild : current.getChildren()) {
         CallTreeNode otherVersionNode = mapping.get(currentChild);
         mapAllNodes(currentChild, otherVersionNode);

         allNodesPredecessor.add(otherVersionNode);
      }
   }

   private void findTreeStructureDiffering(final CallTreeNode current, final CallTreeNode currentPredecessor, BidiMap<CallTreeNode, CallTreeNode> mapping) {
      current.getChildren().forEach(child -> {
         if (child.getCall().equals(CauseSearchData.ADDED)) {
            CallTreeNode otherVersionNode = mapping.get(child);
            treeStructureDiffering.add(otherVersionNode);
         }
      });
      currentPredecessor.getChildren().forEach(child -> {
         if (child.getCall().equals(CauseSearchData.ADDED)) {
            CallTreeNode otherVersionNode = mapping.getKey(child);
            treeStructureDiffering.add(otherVersionNode);
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