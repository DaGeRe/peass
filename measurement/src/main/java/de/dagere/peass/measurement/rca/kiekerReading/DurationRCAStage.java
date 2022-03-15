package de.dagere.peass.measurement.rca.kiekerReading;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.kieker.record.DurationRecord;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kieker.KiekerPatternConverter;
import kieker.analysis.trace.AbstractTraceAnalysisStage;
import kieker.model.repository.SystemModelRepository;

public class DurationRCAStage extends AbstractTraceAnalysisStage<DurationRecord> {

   private static final Logger LOG = LogManager.getLogger(DurationRCAStage.class);

   private final Map<String, CallTreeNode> measuredNodes = new HashMap<>();
   private final String version;

   /**
    * Creates a new instance of this class using the given parameters.
    *
    * @param repository system model repository
    */
   public DurationRCAStage(final SystemModelRepository systemModelRepository, final Set<CallTreeNode> measuredNodes, final String version) {
      super(systemModelRepository);
      for (CallTreeNode node : measuredNodes) {
         this.measuredNodes.put(node.getKiekerPattern(), node);
      }
      this.version = version;

      measuredNodes.forEach(node -> node.newVM(version));
   }

   @Override
   protected void execute(final DurationRecord execution) throws Exception {
      final String kiekerPattern = KiekerPatternConverter.addNewIfRequired(execution.getOperationSignature());
      CallTreeNode node = measuredNodes.get(kiekerPattern);
      if (node != null) {
         // Get duration in mikroseconds - Kieker produces nanoseconds
         final long duration = (execution.getTout() - execution.getTin());
         node.addMeasurement(version, duration);
      }
   }
}
