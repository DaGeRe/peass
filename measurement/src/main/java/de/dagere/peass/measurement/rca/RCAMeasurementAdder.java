package de.dagere.peass.measurement.rca;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.measurement.rca.data.CallTreeNode;

/**
 * If a fixed node list is analyzed (i.e. for {@link RCAStrategy#COMPLETE} and {@link RCAStrategy#UNTIL_SOURCE_CHANGE}), the results are transferred to the {@link CausePersistenceManager}. This is done by the RCAMeasurementAdder.
 *
 */
public class RCAMeasurementAdder {

   private static final Logger LOG = LogManager.getLogger(RCAMeasurementAdder.class);

   private final CausePersistenceManager persistenceManager;
   private final List<CallTreeNode> includableNodes;

   public RCAMeasurementAdder(CausePersistenceManager persistenceManager, List<CallTreeNode> includableNodes) {
      this.persistenceManager = persistenceManager;
      this.includableNodes = includableNodes;
   }

   public void addAllMeasurements(CallTreeNode root) {
      persistenceManager.addMeasurement(root);
      addMeasurements(root);
   }

   private void addMeasurements(final CallTreeNode parent) {
      for (CallTreeNode child : parent.getChildren()) {
         if (includableNodes.contains(child)) {
            LOG.debug("Analyzing: {}", child);
            persistenceManager.addMeasurement(child);
            addMeasurements(child);
         }
      }
   }
}
