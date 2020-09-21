package de.peass.measurement.rca;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.measurement.rca.serialization.MeasuredNode;
import de.peass.measurement.rca.treeanalysis.TreeUtil;

/**
 * Supports continueing of RCA by adding all old measurements into CallTreeNodes
 * @author reichelt
 *
 */
public class LevelManager {

   private static final Logger LOG = LogManager.getLogger(LevelManager.class);

   private final List<CallTreeNode> mainVersionNodeList;
   private final List<CallTreeNode> predecessorNodeList;

   public LevelManager(final List<CallTreeNode> mainVersionNodeList, final List<CallTreeNode> currentPredecessorNodeList, final BothTreeReader reader) {
      this.mainVersionNodeList = mainVersionNodeList;
      this.predecessorNodeList = currentPredecessorNodeList;

      mainVersionNodeList.add(reader.getRootVersion());
      currentPredecessorNodeList.add(reader.getRootPredecessor());
      reader.getRootVersion().setOtherVersionNode(reader.getRootPredecessor());
      reader.getRootPredecessor().setOtherVersionNode(reader.getRootVersion());
   }

   public void goToLastMeasuredLevel(final MeasuredNode root) {
      final List<MeasuredNode> persistentNodes = new LinkedList<>();
      persistentNodes.add(root);

      final Set<CallTreeNode> predecessorNextLevel = new HashSet<>();
      final Set<CallTreeNode> mainNextLevel = new HashSet<>();
      do {
         predecessorNextLevel.clear();
         mainNextLevel.clear();
         LOG.debug("Node: {} {} {}", persistentNodes, predecessorNodeList, mainVersionNodeList);
         final List<MeasuredNode> persistentNextLevel = new LinkedList<>();
         boolean anyHasChild = false;
         for (final MeasuredNode measuredNode : persistentNodes) {
            if (!measuredNode.getChilds().isEmpty()) {
               searchCallTreeNodes(predecessorNextLevel, mainNextLevel, persistentNextLevel, measuredNode);
               anyHasChild = true;
            }
         }
         if (anyHasChild) {
            persistentNodes.clear();
            persistentNodes.addAll(persistentNextLevel);
            predecessorNodeList.clear();
            predecessorNodeList.addAll(predecessorNextLevel);
            mainVersionNodeList.clear();
            mainVersionNodeList.addAll(mainNextLevel);
         }
      } while (!predecessorNextLevel.isEmpty() && !mainNextLevel.isEmpty());
      setLastLevel(mainVersionNodeList, mainNextLevel);
      setLastLevel(predecessorNodeList, predecessorNextLevel);
      LOG.debug("Nodes: {} {}", predecessorNodeList.size(), mainVersionNodeList.size());
   }

   private void setLastLevel(final List<CallTreeNode> nodeList, final Set<CallTreeNode> nextLevel) {
      for (final CallTreeNode processedNode : nodeList) {
         System.out.println("Searching mapping: " + processedNode + " " + processedNode.getOtherVersionNode());
         TreeUtil.findChildMapping(processedNode, processedNode.getOtherVersionNode());
         nextLevel.addAll(processedNode.getChildren());
      }
      nodeList.clear();
      nodeList.addAll(nextLevel);
      nextLevel.clear();
   }

   private void searchCallTreeNodes(final Set<CallTreeNode> predecessorNextLevel, final Set<CallTreeNode> mainNextLevel, final List<MeasuredNode> persistentNextLevel,
         final MeasuredNode measuredNode) {
      for (final MeasuredNode persistentChild : measuredNode.getChilds()) {
         final CallTreeNode reusePredecessor = getSameParent(predecessorNodeList, persistentChild);
         final CallTreeNode reuseMain = getSameParent(mainVersionNodeList, persistentChild);
         if (reuseMain != null && reusePredecessor != null) {
            reusePredecessor.setOtherVersionNode(reuseMain);
            reuseMain.setOtherVersionNode(reusePredecessor);
            mainNextLevel.add(reuseMain);
            predecessorNextLevel.add(reusePredecessor);
            persistentNextLevel.add(persistentChild);
         }
      }
   }

   private CallTreeNode getSameParent(final List<CallTreeNode> currentVersionNodeList, final MeasuredNode persistentChild) {
      CallTreeNode reuse = null;
      for (final CallTreeNode parentNode : currentVersionNodeList) {
         for (final CallTreeNode child : parentNode.getChildren()) {
            if (child.getKiekerPattern().equals(persistentChild.getKiekerPattern())) {
               reuse = child;
               break;
            }
         }
      }
      return reuse;
   }
}