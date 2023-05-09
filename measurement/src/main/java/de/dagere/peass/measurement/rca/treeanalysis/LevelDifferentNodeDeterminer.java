package de.dagere.peass.measurement.rca.treeanalysis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;

/**
 * Determines the differing nodes for one level, and states which nodes need to be analyzed next.
 * 
 */
public class LevelDifferentNodeDeterminer extends DifferentNodeDeterminer {

   private static final Logger LOG = LogManager.getLogger(LevelDifferentNodeDeterminer.class);

   protected final List<CallTreeNode> treeStructureDifferentNodes = new LinkedList<CallTreeNode>();

   public LevelDifferentNodeDeterminer(final List<CallTreeNode> currentPredecessorNodeList, final List<CallTreeNode> currentVersionNodeList,
         final CauseSearcherConfig causeSearchConfig, final MeasurementConfig measurementConfig) {
      super(causeSearchConfig, measurementConfig);
      
      List<CallTreeNode> currentNodes = new LinkedList<>(currentVersionNodeList);
      
      for (CallTreeNode predecessorNode : currentPredecessorNodeList) {
         CallTreeNode currentVersionNode = predecessorNode.getOtherCommitNode();
         if (currentVersionNode == null) {
            throw new RuntimeException("Node " + predecessorNode + " was not mapped");
         }
         currentNodes.remove(currentVersionNode);
         findMeasurable(predecessorNode, currentVersionNode);
      }
      
      if (!currentNodes.isEmpty()) {
         LOG.error("Could not map node lists");
         LOG.error("Predecessor: {}", currentPredecessorNodeList);
         LOG.error("Current: {}", currentVersionNodeList);
         throw new RuntimeException("Mapping error");
      }
   }

   private void findMeasurable(final CallTreeNode currentPredecessorNode, final CallTreeNode currentCommitNode) {
      if (currentPredecessorNode != null && currentCommitNode != null) {
         if (!TreeUtil.childrenEqual(currentPredecessorNode, currentCommitNode)) {
            final int matched = TreeUtil.findChildMapping(currentPredecessorNode, currentCommitNode);
            if (matched > 0) {
               measurePredecessor.add(currentPredecessorNode);
               // needToMeasureCurrent.add(currentVersionNode);
            } else {
               treeStructureDifferentNodes.add(currentPredecessorNode);
               treeStructureDifferentNodes.add(currentCommitNode);
            }
         } else {
            measurePredecessor.add(currentPredecessorNode);
            // needToMeasureCurrent.add(currentVersionNode);
         }
      } else {
         LOG.info("No child node left: {} {}", currentPredecessorNode, currentCommitNode);
      }
   }

   public List<CallTreeNode> getTreeStructureDifferingNodes() {
      return treeStructureDifferentNodes;
   }

   public List<CallTreeNode> getMeasurePredecessor() {
      return measurePredecessor;
   }

}
