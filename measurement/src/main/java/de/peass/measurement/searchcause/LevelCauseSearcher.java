package de.peass.measurement.searchcause;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.searchcause.data.CallTreeNode;
import kieker.analysis.exception.AnalysisConfigurationException;

public class LevelCauseSearcher {

   private static final Logger LOG = LogManager.getLogger(LevelCauseSearcher.class);

   private final List<CallTreeNode> needToMeasurePredecessor = new LinkedList<>();
   private final List<CallTreeNode> needToMeasureCurrent = new LinkedList<>();
   
   private final List<CallTreeNode> differingPredecessor = new LinkedList<>();
   private final List<CallTreeNode> differingCurrent = new LinkedList<>();

   private final List<CallTreeNode> treeStructureDifferingNodes = new LinkedList<CallTreeNode>();
   
   final double confidence = 0.01;

   public LevelCauseSearcher(List<CallTreeNode> currentPredecessorNodeList, List<CallTreeNode> currentVersionNodeList)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      Iterator<CallTreeNode> predecessorIterator = currentPredecessorNodeList.iterator();
      Iterator<CallTreeNode> currentIterator = currentVersionNodeList.iterator();
      for (; currentIterator.hasNext();) {
         CallTreeNode currentPredecessorNode = predecessorIterator.next();
         CallTreeNode currentVersionNode = currentIterator.next();
         if (!TreeUtil.childrenEqual(currentPredecessorNode, currentVersionNode)) {
            treeStructureDifferingNodes.add(currentPredecessorNode);
            treeStructureDifferingNodes.add(currentVersionNode);
         } else {
            needToMeasurePredecessor.add(currentPredecessorNode);
            needToMeasureCurrent.add(currentVersionNode);
         }
      }
   }

   public void calculateDiffering(String predecessor, String version) {
      Iterator<CallTreeNode> predecessorIterator = needToMeasurePredecessor.iterator();
      Iterator<CallTreeNode> currentIterator = needToMeasureCurrent.iterator();
      for (; currentIterator.hasNext();) {
         CallTreeNode currentPredecessorNode = predecessorIterator.next();
         CallTreeNode currentVersionNode = currentIterator.next();
         final DescriptiveStatistics statisticsPredecessor = currentPredecessorNode.getStatistics(predecessor);
         final DescriptiveStatistics statisticsVersion = currentPredecessorNode.getStatistics(version);
         LOG.debug("Comparison {}", currentPredecessorNode.getKiekerPattern());
         LOG.debug("Current: {} {} Predecessor: {} {}", statisticsVersion.getMean(), statisticsVersion.getStandardDeviation(),
               statisticsPredecessor.getMean(), statisticsPredecessor.getStandardDeviation());
         LOG.debug("T={} {}", TestUtils.t(statisticsPredecessor, statisticsVersion), TestUtils.tTest(statisticsPredecessor, statisticsVersion, confidence));
         if (TestUtils.tTest(statisticsPredecessor, statisticsVersion, confidence)) {
            differingPredecessor.addAll(currentPredecessorNode.getChildren());
            differingCurrent.addAll(currentVersionNode.getChildren());
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
