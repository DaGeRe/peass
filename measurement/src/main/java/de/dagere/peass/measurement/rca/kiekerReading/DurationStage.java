package de.dagere.peass.measurement.rca.kiekerReading;

import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kieker.KiekerPatternConverter;
import kieker.analysis.trace.AbstractTraceProcessingStage;
import kieker.model.repository.SystemModelRepository;
import kieker.model.system.model.Execution;

public class DurationStage extends AbstractTraceProcessingStage<Execution> {

   private static final Logger LOG = LogManager.getLogger(DurationStage.class);

   private final Set<CallTreeNode> measuredNodes;
   private final String version;

   public DurationStage(final SystemModelRepository systemModelRepository, final Set<CallTreeNode> measuredNodes, final String version) {
      super(systemModelRepository);
      this.measuredNodes = measuredNodes;
      this.version = version;

      measuredNodes.forEach(node -> node.newVM(version));
   }

   @Override
   protected void execute(final Execution execution) throws Exception {
      LOG.trace("Trace: " + execution.getTraceId());
      addMeasurements(execution);
   }

   private void addMeasurements(final Execution execution) {
      for (final CallTreeNode node : measuredNodes) {
         String kiekerPattern = KiekerPatternConverter.getKiekerPattern(execution.getOperation());
         if (node.getKiekerPattern().equals(kiekerPattern)) {
            // Get duration in mikroseconds - Kieker produces nanoseconds
            final long duration = (execution.getTout() - execution.getTin()) / 1000;
            node.addMeasurement(version, duration);
         }
      }
   }
}
