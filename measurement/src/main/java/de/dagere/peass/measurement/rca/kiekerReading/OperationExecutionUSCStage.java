package de.dagere.peass.measurement.rca.kiekerReading;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kieker.KiekerPatternConverter;
import kieker.analysis.architecture.trace.AbstractTraceProcessingStage;
import kieker.model.repository.SystemModelRepository;
import kieker.model.system.model.Execution;

/**
 * Reads the operations from an UNTIL_SOURCE_CHANGE strategy execution.
 * 
 * This is *not* the optimal solution, as it cannot distinguish between the same method signature being called from different contexts; however, this would require getting the
 * reduced tree from another analysis-execution (like it is currently executed for getting the full tree), which is much more effort, so this is a viable workaround.
 */
public class OperationExecutionUSCStage extends AbstractTraceProcessingStage<Execution> {

   private static final Logger LOG = LogManager.getLogger(OperationExecutionUSCStage.class);

   private final Map<String, CallTreeNode> measuredNodes = new HashMap<>();
   private final String commit;

   public OperationExecutionUSCStage(final SystemModelRepository systemModelRepository, final Set<CallTreeNode> measuredNodes, final String commit) {
      super(systemModelRepository);
      for (CallTreeNode node : measuredNodes) {
         String currentPattern;
         if (node.getConfig().getFixedCommitConfig().getCommitOld().equals(commit)) {
            currentPattern = node.getKiekerPattern();
         } else {
            currentPattern = node.getOtherKiekerPattern();
         }
         this.measuredNodes.put(currentPattern, node);
      }
      this.commit = commit;

      measuredNodes.forEach(node -> node.initVMData(commit));
   }

   @Override
   protected void execute(final Execution execution) throws Exception {
      LOG.trace("Trace: {}", execution.getTraceId());
      addMeasurements(execution);
   }

   private void addMeasurements(final Execution execution) {
      final String kiekerPattern = KiekerPatternConverter.getKiekerPattern(execution.getOperation());
      CallTreeNode node = measuredNodes.get(kiekerPattern);
      if (node != null) {
         // Get duration in mikroseconds - Kieker produces nanoseconds
         final long duration = (execution.getTout() - execution.getTin());
         node.addMeasurement(commit, duration);
      } else {
         LOG.error("Did not find {} Eoi: {} ESS: {} ", kiekerPattern, execution.getEoi(), execution.getEss());
         LOG.info("Nodes: " + measuredNodes.keySet());
      }
   }
}
