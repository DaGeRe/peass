package de.peass.measurement.rca.treeanalysis;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.measurement.analysis.Relation;
import de.peass.measurement.analysis.StatisticUtil;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.data.CallTreeNode;

public abstract class DifferentNodeDeterminer {

   private static final Logger LOG = LogManager.getLogger(DifferentNodeDeterminer.class);

   protected List<CallTreeNode> measurePredecessor = new LinkedList<>();

   protected final List<CallTreeNode> measureNextlevelPredecessor = new LinkedList<>();
   protected final List<CallTreeNode> measureNextLevel = new LinkedList<>();

   protected final List<CallTreeNode> currentLevelDifferent = new LinkedList<>();

   protected final CauseSearcherConfig causeSearchConfig;
   protected final MeasurementConfiguration measurementConfig;

   public DifferentNodeDeterminer(final CauseSearcherConfig causeSearchConfig, final MeasurementConfiguration measurementConfig) {
      this.causeSearchConfig = causeSearchConfig;
      this.measurementConfig = measurementConfig;
   }

   public void calculateDiffering() {
      final Iterator<CallTreeNode> predecessorIterator = measurePredecessor.iterator();
      // final Iterator<CallTreeNode> currentIterator = needToMeasureCurrent.iterator();
      for (; predecessorIterator.hasNext();) {
         final CallTreeNode currentPredecessorNode = predecessorIterator.next();
         // final CallTreeNode currentVersionNode = currentIterator.next();
         final SummaryStatistics statisticsPredecessor = currentPredecessorNode.getStatistics(measurementConfig.getVersionOld());
         final SummaryStatistics statisticsVersion = currentPredecessorNode.getStatistics(measurementConfig.getVersion());
         LOG.debug("Comparison {} - {}",
               currentPredecessorNode.getKiekerPattern(),
               currentPredecessorNode.getOtherVersionNode() != null ? currentPredecessorNode.getOtherVersionNode().getKiekerPattern() : null);
         LOG.debug("Current: {} {} Predecessor: {} {}",
               statisticsVersion.getMean(), statisticsVersion.getStandardDeviation(),
               statisticsPredecessor.getMean(), statisticsPredecessor.getStandardDeviation());
         final Relation relation = StatisticUtil.agnosticTTest(statisticsPredecessor, statisticsVersion, measurementConfig);
         if (relation == Relation.UNEQUAL && needsEnoughTime(statisticsPredecessor, statisticsVersion)) {
            measureNextlevelPredecessor.addAll(currentPredecessorNode.getChildren());
            final List<CallTreeNode> currentNodes = buildCurrentDiffering(currentPredecessorNode);
            measureNextLevel.addAll(currentNodes);

            final int childsRemeasure = getRemeasureChilds(currentPredecessorNode);

            if (childsRemeasure == 0) {
               LOG.debug("Adding {} - no childs needs to be remeasured, T={}", currentPredecessorNode, childsRemeasure, TestUtils.t(statisticsPredecessor, statisticsVersion));
               LOG.debug("Childs: {}", currentPredecessorNode.getChildren());
               currentLevelDifferent.add(currentPredecessorNode);
            }
         }
      }
   }

   private boolean needsEnoughTime(final SummaryStatistics statisticsPredecessor, final SummaryStatistics statisticsVersion) {
      return statisticsPredecessor.getMean() > causeSearchConfig.getMinTime() &&
            statisticsVersion.getMean() > causeSearchConfig.getMinTime();
   }

   private List<CallTreeNode> buildCurrentDiffering(final CallTreeNode currentPredecessorNode) {
      final List<CallTreeNode> currentNodes = new LinkedList<>();
      currentPredecessorNode.getChildren().forEach(node -> currentNodes.add(node.getOtherVersionNode()));
      return currentNodes;
   }

   private int getRemeasureChilds(final CallTreeNode predecessorNode) {
      int childsRemeasure = 0;
      LOG.debug("Children: {}", predecessorNode.getChildren().size());
      for (final CallTreeNode testChild : predecessorNode.getChildren()) {
         LOG.debug("Child: {} Parent: {}", testChild, measureNextlevelPredecessor);
         if (measureNextlevelPredecessor.contains(testChild)) {
            childsRemeasure++;
         }
      }
      return childsRemeasure;
   }

   public List<CallTreeNode> getCurrentLevelDifferent() {
      return currentLevelDifferent;
   }
}