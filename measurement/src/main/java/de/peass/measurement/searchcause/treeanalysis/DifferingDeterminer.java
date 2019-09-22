package de.peass.measurement.searchcause.treeanalysis;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.analysis.Relation;
import de.peass.measurement.analysis.StatisticUtil;
import de.peass.measurement.searchcause.CauseSearcherConfig;
import de.peass.measurement.searchcause.data.CallTreeNode;

public abstract class DifferingDeterminer {

   private static final Logger LOG = LogManager.getLogger(DifferingDeterminer.class);

   protected List<CallTreeNode> needToMeasurePredecessor = new LinkedList<>();
   protected List<CallTreeNode> needToMeasureCurrent = new LinkedList<>();

   protected final List<CallTreeNode> differingPredecessor = new LinkedList<>();
   protected final List<CallTreeNode> differingCurrent = new LinkedList<>();
   
   protected final List<CallTreeNode> measurementDiffering = new LinkedList<>();

   protected final CauseSearcherConfig causeSearchConfig;
   protected final MeasurementConfiguration measurementConfig;

   public DifferingDeterminer(final CauseSearcherConfig causeSearchConfig, final MeasurementConfiguration measurementConfig) {
      this.causeSearchConfig = causeSearchConfig;
      this.measurementConfig = measurementConfig;
   }

   public void calculateDiffering() {
      final Iterator<CallTreeNode> predecessorIterator = needToMeasurePredecessor.iterator();
//      final Iterator<CallTreeNode> currentIterator = needToMeasureCurrent.iterator();
      for (; predecessorIterator.hasNext();) {
         final CallTreeNode currentPredecessorNode = predecessorIterator.next();
//         final CallTreeNode currentVersionNode = currentIterator.next();
         final SummaryStatistics statisticsPredecessor = currentPredecessorNode.getStatistics(measurementConfig.getVersionOld());
         final SummaryStatistics statisticsVersion = currentPredecessorNode.getStatistics(measurementConfig.getVersion());
         LOG.debug("Comparison {} - {}", 
               currentPredecessorNode.getKiekerPattern(), 
               currentPredecessorNode.getOtherVersionNode() != null ? currentPredecessorNode.getOtherVersionNode().getKiekerPattern() : null);
         LOG.debug("Current: {} {} Predecessor: {} {}", statisticsVersion.getMean(), statisticsVersion.getStandardDeviation(),
               statisticsPredecessor.getMean(), statisticsPredecessor.getStandardDeviation());
         final Relation relation = StatisticUtil.agnosticTTest(statisticsPredecessor, statisticsVersion, measurementConfig);
         if (relation == Relation.UNEQUAL) {
            differingPredecessor.addAll(currentPredecessorNode.getChildren());
            final List<CallTreeNode> currentNodes = new LinkedList<>();
            currentPredecessorNode.getChildren().forEach(node -> currentNodes.add(node.getOtherVersionNode()));
            differingCurrent.addAll(currentNodes);
         }
      }
   }
   
   public List<CallTreeNode> getMeasurementDiffering() {
      return measurementDiffering;
   }
   
   public void analyseNode(final CallTreeNode predecessorNode) {
      final SummaryStatistics statisticsPredecessor = predecessorNode.getStatistics(measurementConfig.getVersionOld());
      final SummaryStatistics statisticsVersion = predecessorNode.getStatistics(measurementConfig.getVersion());
      final Relation relation = StatisticUtil.agnosticTTest(statisticsPredecessor, statisticsVersion, measurementConfig);
      LOG.debug("Relation: {} Call: {}", relation, predecessorNode.getCall());
      if (relation == Relation.UNEQUAL) {
         final int childsRemeasure = getRemeasureChilds(predecessorNode);

         if (childsRemeasure == 0) {
            LOG.debug("Adding {} - no childs needs to be remeasured, T={}", predecessorNode, childsRemeasure, TestUtils.t(statisticsPredecessor, statisticsVersion));
            LOG.debug("Childs: {}", predecessorNode.getChildren());
            measurementDiffering.add(predecessorNode);
         }
      }
   }

   private int getRemeasureChilds(final CallTreeNode predecessorNode) {
      int childsRemeasure = 0;
      LOG.debug("Children: {}", predecessorNode.getChildren().size());
      for (final CallTreeNode testChild : predecessorNode.getChildren()) {
         LOG.debug("Child: {} Parent: {}", testChild, differingPredecessor);
         if (differingPredecessor.contains(testChild)) {
            childsRemeasure++;
         }
      }
      return childsRemeasure;
   }
}