package de.peass.measurement.rca.treeanalysis;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.config.MeasurementConfiguration;
import de.peass.measurement.analysis.Relation;
import de.peass.measurement.analysis.StatisticUtil;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.data.CallTreeNode;
import de.precision.analysis.repetitions.bimodal.CompareData;
import de.precision.analysis.repetitions.bimodal.OutlierRemoverBimodal;

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
         CompareData cd = currentPredecessorNode.getComparableStatistics(measurementConfig.getVersionOld(), measurementConfig.getVersion());
         if (measurementConfig.getStatisticsConfig().getOutlierFactor() != 0) {
            CompareData cleaned = OutlierRemoverBimodal.removeOutliers(cd);
            calculateNodeDifference(currentPredecessorNode, cleaned);
         } else {
            calculateNodeDifference(currentPredecessorNode, cd);
         }
      }
   }

   private void calculateNodeDifference(final CallTreeNode currentPredecessorNode, final CompareData cd) {
      if (cd.getBeforeStat() == null || cd.getAfterStat() == null) {
         LOG.debug("Statistics is null, is different: {} vs {}", cd.getBeforeStat(), cd.getAfterStat());
         currentLevelDifferent.add(currentPredecessorNode);
      } else {
         printComparisonInfos(currentPredecessorNode, cd.getBeforeStat(), cd.getAfterStat());
         if (cd.getBeforeStat().getN() > 0 && cd.getAfterStat().getN() > 0) {
            final Relation relation = StatisticUtil.isDifferent(cd, measurementConfig);
            LOG.debug("Relation: {}", relation);
            if (relation == Relation.UNEQUAL && needsEnoughTime(cd.getBeforeStat(), cd.getAfterStat())) {
               addChildsToMeasurement(currentPredecessorNode, cd.getBeforeStat(), cd.getAfterStat());
            } else {
               LOG.info("No remeasurement");
            }
         }
      }
   }

   private void printComparisonInfos(final CallTreeNode currentPredecessorNode, final SummaryStatistics statisticsPredecessor, final SummaryStatistics statisticsVersion) {
      LOG.debug("Comparison {} - {}",
            currentPredecessorNode.getKiekerPattern(),
            currentPredecessorNode.getOtherVersionNode() != null ? currentPredecessorNode.getOtherVersionNode().getKiekerPattern() : null);
      LOG.debug("Predecessor: {} {} Current: {} {} ",
            statisticsPredecessor.getMean(), statisticsPredecessor.getStandardDeviation(),
            statisticsVersion.getMean(), statisticsVersion.getStandardDeviation());
   }

   private void addChildsToMeasurement(final CallTreeNode currentPredecessorNode, final SummaryStatistics statisticsPredecessor, final SummaryStatistics statisticsVersion) {
      measureNextlevelPredecessor.addAll(currentPredecessorNode.getChildren());
      final List<CallTreeNode> currentNodes = buildCurrentDiffering(currentPredecessorNode);
      measureNextLevel.addAll(currentNodes);

      final int childsRemeasure = getRemeasureChilds(currentPredecessorNode);

      if (childsRemeasure == 0) {
         LOG.debug("Adding {} - no childs needs to be remeasured, T={}", currentPredecessorNode, childsRemeasure,
               TestUtils.homoscedasticT(statisticsPredecessor, statisticsVersion));
         LOG.debug("Childs: {}", currentPredecessorNode.getChildren());
         currentLevelDifferent.add(currentPredecessorNode);
      }
   }

   private boolean needsEnoughTime(final SummaryStatistics statisticsPredecessor, final SummaryStatistics statisticsVersion) {
      double relativeDifference = Math.abs(statisticsPredecessor.getMean() - statisticsVersion.getMean()) / statisticsVersion.getMean();
      double relativeDeviationPredecessor = statisticsPredecessor.getStandardDeviation() / statisticsPredecessor.getMean();
      double relativeDeviationVersion = statisticsVersion.getStandardDeviation() / statisticsVersion.getMean();
      double relativeStandardDeviation = Math.sqrt((Math.pow(relativeDeviationPredecessor, 2) +
            Math.pow(relativeDeviationVersion, 2)) / 2);
      return relativeDifference > causeSearchConfig.getMinTime() * relativeStandardDeviation;
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