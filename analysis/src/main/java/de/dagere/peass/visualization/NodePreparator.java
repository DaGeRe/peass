package de.dagere.peass.visualization;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.serialization.MeasuredNode;
import de.dagere.peass.measurement.rca.serialization.MeasuredValues;
import de.dagere.peass.measurement.rca.treeanalysis.TreeUtil;
import de.dagere.peass.visualization.GraphNode.State;

public class NodePreparator implements INodePreparator {

   public static final String COLOR_FASTER = "#00FF00";
   public static final String COLOR_SLOWER = "#FF0000";

   private static final Logger LOG = LogManager.getLogger(NodePreparator.class);

   private CallTreeNode rootPredecessor, rootCurrent;
   private final CauseSearchData data;
   private final GraphNode rootGraph;

   public NodePreparator(final CallTreeNode rootPredecessor, final CallTreeNode rootCurrent, final CauseSearchData data) {
      this.rootPredecessor = rootPredecessor;
      this.rootCurrent = rootCurrent;
      this.data = data;

      MeasuredNode measuredRootNode = data.getNodes();
      rootGraph = new GraphNode(measuredRootNode.getCall(), measuredRootNode.getKiekerPattern(), measuredRootNode.getOtherKiekerPattern());
      rootGraph.setModule(measuredRootNode.getModule());

      if (rootPredecessor != null && !measuredRootNode.getCall().equals(rootPredecessor.getCall())) {
         throw new RuntimeException("Internal error happened: Meaured node had call " + measuredRootNode.getCall() +
               " but structure node had call " + rootPredecessor.getCall());
      }
   }

   public NodePreparator(final CauseSearchData data) {
      this(null, null, data);
   }

   public void prepare() {
      final MeasuredNode parent = data.getNodes();
      setGraphData(parent, rootGraph);

      processNode(parent, rootGraph);

      if (rootPredecessor != null && rootCurrent != null) {
         handleFullTreeNode(rootGraph, rootPredecessor, rootCurrent);
      }

      PrefixSetter.preparePrefix(rootGraph);
   }

   private void handleFullTreeNode(final GraphNode graphNode, final CallTreeNode nodePredecessor, final CallTreeNode nodeCurrent) {
      if (graphNode.getChildren().size() > 0) {
         handleMeasuredTriple(graphNode, nodePredecessor, nodeCurrent);
      } else {
         handleUnmeasuredNode(graphNode, nodePredecessor, nodeCurrent);
      }

   }

   private void handleUnmeasuredNode(final GraphNode graphNode, final CallTreeNode nodePredecessor, final CallTreeNode nodeCurrent) {
      if (nodePredecessor != null && nodeCurrent != null) {
         TreeUtil.findChildMapping(nodePredecessor, nodeCurrent);
      }

      final Set<String> addedEmptyChildren = new HashSet<>();
      for (final CallTreeNode purePredecessorChild : nodePredecessor.getChildren()) {
         boolean add = true;
         if (purePredecessorChild.getChildren().size() == 0) {
            if (addedEmptyChildren.contains(purePredecessorChild.getKiekerPattern())) {
               add = false;
            } else {
               addedEmptyChildren.add(purePredecessorChild.getKiekerPattern());
            }
         }
         if (add) {
            final GraphNode newChild = new GraphNode(purePredecessorChild.getCall(), purePredecessorChild.getKiekerPattern(), purePredecessorChild.getOtherKiekerPattern());
            newChild.setModule(purePredecessorChild.getModule());
            newChild.setName(purePredecessorChild.getCall());
            newChild.setColor("#5555FF");
            newChild.setState(State.UNKNOWN);
            graphNode.getChildren().add(newChild);
            newChild.setEss(-1);
            LOG.trace("Adding: " + purePredecessorChild.getCall() + " Parent: " + graphNode.getKiekerPattern());
            handleFullTreeNode(newChild, purePredecessorChild, purePredecessorChild.getOtherCommitNode());
         }
      }
   }

   private void handleMeasuredTriple(final GraphNode graphNode, final CallTreeNode nodePredecessor, final CallTreeNode nodeCurrent) {
      if (nodePredecessor != null && nodeCurrent != null) {
         TreeUtil.findChildMapping(nodePredecessor, nodeCurrent);
      }

      System.out.println("Handling: " + graphNode + " " + nodePredecessor + " " + nodeCurrent);
      for (int index = 0; index < graphNode.getChildren().size(); index++) {
         final GraphNode graphChild = graphNode.getChildren().get(index);
         final CallTreeNode purePredecessorChild;
         if (nodePredecessor != null && nodePredecessor.getChildren().size() > index) {
            purePredecessorChild = nodePredecessor.getChildren().get(index);
         } else {
            purePredecessorChild = null;
         }
         handleFullTreeNode(graphChild, purePredecessorChild, (purePredecessorChild != null ? purePredecessorChild.getOtherCommitNode() : null));
      }
   }

   private void processNode(final MeasuredNode measuredParent, final GraphNode graphParent) {
      // final EqualChildDeterminer determiner = new EqualChildDeterminer();
      final Set<String> addedPatterns = new HashSet<>();
      for (final MeasuredNode measuredChild : measuredParent.getChilds()) {
         // if (!determiner.hasEqualSubtreeNode(measuredChild)) {
         if ((Double.isNaN(measuredChild.getStatistic().getMeanCurrent()) && Double.isNaN(measuredChild.getStatistic().getMeanOld())) ||
               (measuredChild.getStatistic().getCalls() == 0 && measuredChild.getStatistic().getCallsOld() == 0)) {
            LOG.debug("Node {} - {} is ignored", measuredChild.getKiekerPattern(), measuredChild.getOtherKiekerPattern());
         } else {
            final GraphNode newChild = createGraphNode(measuredChild);
            if (measuredChild.getCall().equals(CauseSearchData.ADDED) || measuredChild.getOtherKiekerPattern().equals(CauseSearchData.ADDED)) {
               newChild.setHasSourceChange(true);
            }

            newChild.setParent(measuredParent.getCall());
            newChild.setEss(graphParent.getEss() + 1);
            setGraphData(measuredChild, newChild);

            graphParent.getChildren().add(newChild);
            processNode(measuredChild, newChild);
            addedPatterns.add(measuredChild.getKiekerPattern());
         }

      }
      // }
   }

   private GraphNode createGraphNode(final MeasuredNode measuredChild) {
      final GraphNode newChild;
      if (measuredChild.getCall().equals(CauseSearchData.ADDED)) {
         final String pattern = measuredChild.getOtherKiekerPattern();
         String name = pattern.substring(pattern.lastIndexOf(' '), pattern.lastIndexOf('('));
         newChild = new GraphNode(name, pattern, measuredChild.getKiekerPattern());
      } else {
         newChild = new GraphNode(measuredChild.getCall(), measuredChild.getKiekerPattern(), measuredChild.getOtherKiekerPattern());
      }
      newChild.setModule(measuredChild.getModule());
      return newChild;
   }

   private void setGraphData(final MeasuredNode measuredChild, final GraphNode newChild) {
      newChild.setName(measuredChild.getCall());
      setNodeColor(measuredChild, newChild);
      if (measuredChild.getValues() != null) {
         final double[] values = getValueArray(measuredChild.getValues());
         newChild.setValues(values);
         newChild.setVmValues(measuredChild.getValues());
      }
      if (measuredChild.getValuesPredecessor() != null) {
         final double[] values = getValueArray(measuredChild.getValuesPredecessor());
         newChild.setValuesPredecessor(values);
         newChild.setVmValuesPredecessor(measuredChild.getValuesPredecessor());
      }
   }

   private double[] getValueArray(final MeasuredValues measured) {
      return measured.getValuesArray();
   }

   private void setNodeColor(final MeasuredNode measuredNode, final GraphNode graphNode) {
      graphNode.setStatistic(measuredNode.getStatistic());
      // final boolean isChange = StatisticUtil.agnosticTTest(measuredNode.getStatistic().getStatisticsOld(), measuredNode.getStatistic().getStatisticsCurrent(), data.getConfig())
      // == Relation.UNEQUAL;
      final StatisticalSummary statisticsOld = measuredNode.getStatistic().getStatisticsOld();
      final StatisticalSummary statisticsCurrent = measuredNode.getStatistic().getStatisticsCurrent();
      try {
         setColorFullStatistics(measuredNode, graphNode, statisticsOld, statisticsCurrent);
      } catch (Exception e) {
         throw new RuntimeException("Could not examine change " + measuredNode.getKiekerPattern() + " " + measuredNode.getOtherKiekerPattern(), e);
      }

      setInVMDeviations(measuredNode, graphNode);
   }

   private void setInVMDeviations(final MeasuredNode measuredNode, final GraphNode graphNode) {
      SummaryStatistics statisticPredecessor = getInVMDeviationStatistic(measuredNode.getValuesPredecessor().getValues());
      graphNode.setInVMDeviationPredecessor(statisticPredecessor.getMean());
      SummaryStatistics statistic = getInVMDeviationStatistic(measuredNode.getValues().getValues());
      graphNode.setInVMDeviation(statistic.getMean());
   }

   private SummaryStatistics getInVMDeviationStatistic(final Map<Integer, List<StatisticalSummary>> values) {
      SummaryStatistics statistic = new SummaryStatistics();
      for (List<StatisticalSummary> oneVMRun : values.values()) {
         SummaryStatistics vmAverage = new SummaryStatistics();
         for (StatisticalSummary aggregatedRunValues : oneVMRun) {
            vmAverage.addValue(aggregatedRunValues.getMean());
         }
         statistic.addValue(vmAverage.getStandardDeviation());
      }
      return statistic;
   }

   private void setColorFullStatistics(final MeasuredNode measuredNode, final GraphNode graphNode, final StatisticalSummary statisticsOld,
         final StatisticalSummary statisticsCurrent) {
      if (measuredNode.getStatistic().getMeanCurrent() > 0.001 && measuredNode.getStatistic().getMeanOld() > 0.001) {
         final boolean isChange = measuredNode.isChange(0.01);
         if (isChange) {
            if (measuredNode.getStatistic().getTvalue() < 0) {
               graphNode.setSlower();
            } else {
               graphNode.setFaster();
            }
         }
      } else if (Double.isNaN(statisticsOld.getMean()) && !Double.isNaN(statisticsCurrent.getMean())) {
         graphNode.setSlower();
      } else if (!Double.isNaN(statisticsOld.getMean()) && Double.isNaN(statisticsCurrent.getMean())) {
         graphNode.setFaster();
      } else {
         graphNode.setColor("#FFFFFF");
         graphNode.getStatistic().setChange(false);
      }
   }

   @Override
   public GraphNode getGraphRoot() {
      return rootGraph;
   }
}
