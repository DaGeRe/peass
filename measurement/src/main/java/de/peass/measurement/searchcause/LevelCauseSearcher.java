package de.peass.measurement.searchcause;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.analysis.Relation;
import de.peass.measurement.analysis.StatisticUtil;
import de.peass.measurement.searchcause.data.CallTreeNode;
import kieker.analysis.exception.AnalysisConfigurationException;

abstract class DifferingDeterminer {

   private static final Logger LOG = LogManager.getLogger(DifferingDeterminer.class);

   protected List<CallTreeNode> needToMeasurePredecessor = new LinkedList<>();
   protected List<CallTreeNode> needToMeasureCurrent = new LinkedList<>();

   protected final List<CallTreeNode> differingPredecessor = new LinkedList<>();
   protected final List<CallTreeNode> differingCurrent = new LinkedList<>();
   
   protected final List<CallTreeNode> measurementDiffering = new LinkedList<>();

   protected final CauseSearcherConfig causeSearchConfig;
   protected final MeasurementConfiguration measurementConfig;

   public DifferingDeterminer(CauseSearcherConfig causeSearchConfig, MeasurementConfiguration measurementConfig) {
      this.causeSearchConfig = causeSearchConfig;
      this.measurementConfig = measurementConfig;
   }

   public void calculateDiffering() {
      final Iterator<CallTreeNode> predecessorIterator = needToMeasurePredecessor.iterator();
      final Iterator<CallTreeNode> currentIterator = needToMeasureCurrent.iterator();
      for (; currentIterator.hasNext();) {
         final CallTreeNode currentPredecessorNode = predecessorIterator.next();
         final CallTreeNode currentVersionNode = currentIterator.next();
         final SummaryStatistics statisticsPredecessor = currentPredecessorNode.getStatistics(causeSearchConfig.getPredecessor());
         final SummaryStatistics statisticsVersion = currentPredecessorNode.getStatistics(causeSearchConfig.getVersion());
         LOG.debug("Comparison {}", currentPredecessorNode.getKiekerPattern());
         LOG.debug("Current: {} {} Predecessor: {} {}", statisticsVersion.getMean(), statisticsVersion.getStandardDeviation(),
               statisticsPredecessor.getMean(), statisticsPredecessor.getStandardDeviation());
         final Relation relation = StatisticUtil.agnosticTTest(statisticsVersion, statisticsPredecessor, measurementConfig);
         if (relation == Relation.UNEQUAL) {
            differingPredecessor.addAll(currentPredecessorNode.getChildren());
            differingCurrent.addAll(currentVersionNode.getChildren());
         }
      }
   }
   
   public List<CallTreeNode> getMeasurementDiffering() {
      return measurementDiffering;
   }
   
   public void analyseNode(CallTreeNode predecessorNode) {
      final SummaryStatistics statisticsPredecessor = predecessorNode.getStatistics(causeSearchConfig.getPredecessor());
      final SummaryStatistics statisticsVersion = predecessorNode.getStatistics(causeSearchConfig.getVersion());
      final Relation relation = StatisticUtil.agnosticTTest(statisticsPredecessor, statisticsVersion, measurementConfig);
      LOG.debug("Relation: {} Call: {}", relation, predecessorNode.getCall());
      if (relation.equals(Relation.UNEQUAL)) {
         int childsRemeasure = 0;
         for (CallTreeNode testChild : predecessorNode.getChildren()) {
            LOG.debug("Testing: " + testChild + " " + differingPredecessor);
            if (differingPredecessor.contains(testChild)) {
               childsRemeasure++;
            }
         }

         if (childsRemeasure == 0) {
            LOG.debug("Adding {} - {} childs needs to be remeasured, T={}", predecessorNode, childsRemeasure, TestUtils.t(statisticsPredecessor, statisticsVersion));
            LOG.debug("Childs: {}", predecessorNode.getChildren());
            measurementDiffering.add(predecessorNode);
         }
      }
   }
}

class AllCauseSearcher extends DifferingDeterminer {

   public AllCauseSearcher(List<CallTreeNode> needToMeasurePredecessor, List<CallTreeNode> needToMeasureCurrent, CauseSearcherConfig causeSearchConfig,
         MeasurementConfiguration measurementConfig) {
      super(causeSearchConfig, measurementConfig);
      this.needToMeasureCurrent = needToMeasureCurrent;
      this.needToMeasurePredecessor = needToMeasurePredecessor;
   }

}

public class LevelCauseSearcher extends DifferingDeterminer {

   private static final Logger LOG = LogManager.getLogger(LevelCauseSearcher.class);

   protected final List<CallTreeNode> treeStructureDifferingNodes = new LinkedList<CallTreeNode>();

   public LevelCauseSearcher(List<CallTreeNode> currentPredecessorNodeList, List<CallTreeNode> currentVersionNodeList, CauseSearcherConfig causeSearchConfig,
         MeasurementConfiguration measurementConfig)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
      super(causeSearchConfig, measurementConfig);
      final Iterator<CallTreeNode> predecessorIterator = currentPredecessorNodeList.iterator();
      final Iterator<CallTreeNode> currentIterator = currentVersionNodeList.iterator();
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

   public void analyseNode(CallTreeNode predecessorNode) {
      final SummaryStatistics statisticsPredecessor = predecessorNode.getStatistics(causeSearchConfig.getPredecessor());
      final SummaryStatistics statisticsVersion = predecessorNode.getStatistics(causeSearchConfig.getVersion());
      final Relation relation = StatisticUtil.agnosticTTest(statisticsPredecessor, statisticsVersion, measurementConfig);
      LOG.debug("Relation: {} Call: {}", relation, predecessorNode.getCall());
      if (relation.equals(Relation.UNEQUAL)) {
         int childsRemeasure = 0;
         for (CallTreeNode testChild : predecessorNode.getChildren()) {
            LOG.debug("Testing: " + testChild + " " + getDifferingPredecessor());
            if (getDifferingPredecessor().contains(testChild)) {
               childsRemeasure++;
            }
         }

         if (childsRemeasure == 0) {
            LOG.debug("Adding {} - {} childs needs to be remeasured, T={}", predecessorNode, childsRemeasure, TestUtils.t(statisticsPredecessor, statisticsVersion));
            LOG.debug("Childs: {}", predecessorNode.getChildren());
            measurementDiffering.add(predecessorNode);
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
