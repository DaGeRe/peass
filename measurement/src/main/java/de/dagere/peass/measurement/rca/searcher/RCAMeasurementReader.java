package de.dagere.peass.measurement.rca.searcher;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.measurement.rca.CausePersistenceManager;
import de.dagere.peass.measurement.rca.data.CallTreeNode;

/**
 * If a fixed node list is analyzed (i.e. for COMPLETE and UNTIL_SOURCE_CHANGE), the results are read after all measurement is finished. This is done by the MeasurementReader.
 *
 */
public class RCAMeasurementReader {

   private static final Logger LOG = LogManager.getLogger(RCAMeasurementReader.class);

   private final CausePersistenceManager persistenceManager;
   private final List<CallTreeNode> includableNodes;

   public RCAMeasurementReader(CausePersistenceManager persistenceManager, List<CallTreeNode> includableNodes) {
      this.persistenceManager = persistenceManager;
      this.includableNodes = includableNodes;
   }

   public void addAllMeasurements(CallTreeNode root) {
      persistenceManager.addMeasurement(root);
      addMeasurements(root);
   }

   public void addMeasurements(final CallTreeNode parent) {
      for (CallTreeNode child : parent.getChildren()) {
         if (includableNodes.contains(child)) {
            LOG.debug("Analyzing: {}", child);
            persistenceManager.addMeasurement(child);
            addMeasurements(child);
         }
      }
   }
}
