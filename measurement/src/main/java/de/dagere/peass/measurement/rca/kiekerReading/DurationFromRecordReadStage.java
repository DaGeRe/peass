package de.dagere.peass.measurement.rca.kiekerReading;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.kieker.record.DurationRecord;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kieker.KiekerPatternConverter;
import kieker.analysis.trace.AbstractTraceAnalysisStage;
import kieker.model.repository.SystemModelRepository;

public class DurationFromRecordReadStage extends AbstractTraceAnalysisStage<DurationRecord> {

   private static final Logger LOG = LogManager.getLogger(DurationStage.class);

   private final Set<CallTreeNode> measuredNodes;
   private final String version;

   /**
    * Creates a new instance of this class using the given parameters.
    *
    * @param repository system model repository
    */
   public DurationFromRecordReadStage(final SystemModelRepository systemModelRepository, final Set<CallTreeNode> measuredNodes, final String version) {
      super(systemModelRepository);
      this.measuredNodes = measuredNodes;
      this.version = version;

      measuredNodes.forEach(node -> node.newVM(version));
   }

   @Override
   protected void execute(final DurationRecord execution) throws Exception {
      for (final CallTreeNode node : measuredNodes) {
         String kiekerPattern = KiekerPatternConverter.addNewIfRequired( execution.getOperationSignature());
         
         if (node.getKiekerPattern().equals(kiekerPattern)) {
            // Get duration in mikroseconds - Kieker produces nanoseconds
            final long duration = (execution.getTout() - execution.getTin()) / 1000;
            node.addMeasurement(version, duration);
         }
      }
   }
}
