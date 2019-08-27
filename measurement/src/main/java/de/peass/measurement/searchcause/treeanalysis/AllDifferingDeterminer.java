package de.peass.measurement.searchcause.treeanalysis;

import java.util.List;

import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.searchcause.CauseSearcherConfig;
import de.peass.measurement.searchcause.data.CallTreeNode;

/**
 * Determines the differing nodes analyzing the whole tree at once
 * @author reichelt
 *
 */
public class AllDifferingDeterminer extends DifferingDeterminer {

   public AllDifferingDeterminer(final List<CallTreeNode> needToMeasurePredecessor, final List<CallTreeNode> needToMeasureCurrent, final CauseSearcherConfig causeSearchConfig,
         final MeasurementConfiguration measurementConfig) {
      super(causeSearchConfig, measurementConfig);
      this.needToMeasureCurrent = needToMeasureCurrent;
      this.needToMeasurePredecessor = needToMeasurePredecessor;
   }

   public static void main(final String[] args) {
      final String call =  "de.test.CalleeTest#onlyCallMethod1".replace("#", ".");
      final String kieker = "de.test.CalleeTest.onlyCallMethod1()";
      System.out.println(kieker.contains(call));
   }
}