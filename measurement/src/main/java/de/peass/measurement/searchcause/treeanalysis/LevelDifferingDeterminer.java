package de.peass.measurement.searchcause.treeanalysis;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.searchcause.CauseSearcherConfig;
import de.peass.measurement.searchcause.data.CallTreeNode;
import kieker.analysis.exception.AnalysisConfigurationException;

/**
 * Determines the differing nodes for one level, and states which nodes need to be analyzed next.
 * @author reichelt
 *
 */
public class LevelDifferingDeterminer extends DifferingDeterminer {

   private static final Logger LOG = LogManager.getLogger(LevelDifferingDeterminer.class);

   protected final List<CallTreeNode> treeStructureDifferingNodes = new LinkedList<CallTreeNode>();

   public LevelDifferingDeterminer(final List<CallTreeNode> currentPredecessorNodeList, final List<CallTreeNode> currentVersionNodeList, final CauseSearcherConfig causeSearchConfig,
         final MeasurementConfiguration measurementConfig)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      super(causeSearchConfig, measurementConfig);
      final Iterator<CallTreeNode> predecessorIterator = currentPredecessorNodeList.iterator();
      final Iterator<CallTreeNode> currentIterator = currentVersionNodeList.iterator();
      for (; currentIterator.hasNext();) {
         final CallTreeNode currentPredecessorNode = predecessorIterator.next();
         final CallTreeNode currentVersionNode = currentIterator.next();
         if (!TreeUtil.childrenEqual(currentPredecessorNode, currentVersionNode)) {
            final int matched = TreeUtil.findMapping(currentPredecessorNode, currentVersionNode);
            if (matched > 0) {
               needToMeasurePredecessor.add(currentPredecessorNode);
               needToMeasureCurrent.add(currentVersionNode);
            }else {
               treeStructureDifferingNodes.add(currentPredecessorNode);
               treeStructureDifferingNodes.add(currentVersionNode);
            }
//            for (final CallTreeNode child : currentPredecessorNode.getChildren()) {
//               if (child.getOtherVersionNode() != null) {
//                  
//               }
//            }
         } else {
            needToMeasurePredecessor.add(currentPredecessorNode);
            needToMeasureCurrent.add(currentVersionNode);
         }
      }
   }

   public List<CallTreeNode> getDifferingPredecessor() {
      return differingPredecessor;
   }

   public List<CallTreeNode> getDifferingCurrent() {
      return differingCurrent;
   }

   public List<CallTreeNode> getTreeStructureDifferingNodes() {
      return treeStructureDifferingNodes;
   }

   public List<CallTreeNode> getNeedToMeasurePredecessor() {
      return needToMeasurePredecessor;
   }

   public List<CallTreeNode> getNeedToMeasureCurrent() {
      return needToMeasureCurrent;
   }

}
