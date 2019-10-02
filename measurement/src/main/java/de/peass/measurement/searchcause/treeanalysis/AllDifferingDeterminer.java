package de.peass.measurement.searchcause.treeanalysis;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.measurement.searchcause.CauseSearcherConfig;
import de.peass.measurement.searchcause.data.CallTreeNode;

/**
 * Determines the differing nodes analyzing the whole tree at once
 * 
 * @author reichelt
 *
 */
public class AllDifferingDeterminer extends DifferentNodeDeterminer {

   public AllDifferingDeterminer(final List<CallTreeNode> needToMeasurePredecessor, final CauseSearcherConfig causeSearchConfig,
         final MeasurementConfiguration measurementConfig) {
      super(causeSearchConfig, measurementConfig);
//      this.needToMeasureCurrent = needToMeasureCurrent;
      this.measurePredecessor = needToMeasurePredecessor;
   }

   public List<CallTreeNode> getIncludableNodes() {
      final List<CallTreeNode> includeable = new LinkedList<CallTreeNode>();
      for (final CallTreeNode node : measurePredecessor) {
         final SummaryStatistics statistics = node.getStatistics(measurementConfig.getVersion());
         final SummaryStatistics statisticsOld = node.getStatistics(measurementConfig.getVersionOld());
         if (statistics.getMean() > causeSearchConfig.getMinTime() &&
               statisticsOld.getMean() > causeSearchConfig.getMinTime()) {
            includeable.add(node);
         }
      }
      return includeable;

   }
}