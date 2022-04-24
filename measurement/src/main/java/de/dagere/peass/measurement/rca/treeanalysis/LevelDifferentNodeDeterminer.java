package de.dagere.peass.measurement.rca.treeanalysis;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;

import kieker.analysis.exception.AnalysisConfigurationException;

/**
 * Determines the differing nodes for one level, and states which nodes need to be analyzed next.
 * 
 * @author reichelt
 *
 */
public class LevelDifferentNodeDeterminer extends DifferentNodeDeterminer {

   private static final Logger LOG = LogManager.getLogger(LevelDifferentNodeDeterminer.class);

   protected final List<CallTreeNode> treeStructureDifferentNodes = new LinkedList<CallTreeNode>();

   public LevelDifferentNodeDeterminer(final List<CallTreeNode> currentPredecessorNodeList, final List<CallTreeNode> currentVersionNodeList,
         final CauseSearcherConfig causeSearchConfig,
         final MeasurementConfig measurementConfig)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException {
      super(causeSearchConfig, measurementConfig);
      final Iterator<CallTreeNode> predecessorIterator = currentPredecessorNodeList.iterator();
      final Iterator<CallTreeNode> currentIterator = currentVersionNodeList.iterator();
      for (; predecessorIterator.hasNext() && currentIterator.hasNext();) {
         final CallTreeNode currentPredecessorNode = predecessorIterator.next();
         final CallTreeNode currentVersionNode = currentIterator.next();
         findMeasurable(currentPredecessorNode, currentVersionNode);
      }
   }

   private void findMeasurable(final CallTreeNode currentPredecessorNode, final CallTreeNode currentVersionNode) {
      if (currentPredecessorNode != null && currentVersionNode != null) {
         if (!TreeUtil.childrenEqual(currentPredecessorNode, currentVersionNode)) {
            final int matched = TreeUtil.findChildMapping(currentPredecessorNode, currentVersionNode);
            if (matched > 0) {
               measurePredecessor.add(currentPredecessorNode);
//                  needToMeasureCurrent.add(currentVersionNode);
            } else {
               treeStructureDifferentNodes.add(currentPredecessorNode);
               treeStructureDifferentNodes.add(currentVersionNode);
            }
         } else {
            measurePredecessor.add(currentPredecessorNode);
//               needToMeasureCurrent.add(currentVersionNode);
         }
      } else {
         LOG.info("No child node left: {} {}", currentPredecessorNode, currentVersionNode);
      }
   }

//   public List<CallTreeNode> getMeasureNextLevelPredecessor() {
//      return measureNextlevelPredecessor;
//   }
//
//   public List<CallTreeNode> getMeasureNextLevel() {
//      return measureNextLevel;
//   }

   public List<CallTreeNode> getTreeStructureDifferingNodes() {
      return treeStructureDifferentNodes;
   }

   public List<CallTreeNode> getMeasurePredecessor() {
      return measurePredecessor;
   }


}
