package de.dagere.peass.measurement.rca.kiekerReading;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kieker.KiekerPatternConverter;
import kieker.analysis.trace.AbstractTraceProcessingStage;
import kieker.model.repository.SystemModelRepository;
import kieker.model.system.model.Execution;

class EOIESSIndex {
   private final int ess;
   private final int eoi;
   private final String kiekerPattern;

   public EOIESSIndex(int ess, int eoi, String kiekerPattern) {
      this.ess = ess;
      this.eoi = eoi;
      this.kiekerPattern = kiekerPattern;
   }

   @Override
   public int hashCode() {
      return ess + eoi + kiekerPattern.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof EOIESSIndex) {
         EOIESSIndex other = (EOIESSIndex) obj;
         return other.ess == ess &&
               other.eoi == eoi &&
               other.kiekerPattern.equals(kiekerPattern);
      } else {
         return false;
      }
   }

   @Override
   public String toString() {
      return eoi + "-" + ess + "-" + kiekerPattern;
   }
}

public class OperationExecutionRCAStage extends AbstractTraceProcessingStage<Execution> {

   private static final Logger LOG = LogManager.getLogger(OperationExecutionRCAStage.class);

   private final Map<EOIESSIndex, CallTreeNode> measuredNodes = new HashMap<>();
   private final String version;

   public OperationExecutionRCAStage(final SystemModelRepository systemModelRepository, final Set<CallTreeNode> measuredNodes, final String version) {
      super(systemModelRepository);
      for (CallTreeNode node : measuredNodes) {
         int eoi = node.getEoi(version);
         int ess = node.getEss();
         String currentPattern;
         if (node.getConfig().getExecutionConfig().getVersionOld().equals(version)) {
            currentPattern = node.getKiekerPattern();
         } else {
            currentPattern = node.getOtherKiekerPattern();
         }
         EOIESSIndex index = new EOIESSIndex(ess, eoi, currentPattern);
         this.measuredNodes.put(index, node);
      }
      this.version = version;

      measuredNodes.forEach(node -> node.newVM(version));
   }

   @Override
   protected void execute(final Execution execution) throws Exception {
      LOG.trace("Trace: {}", execution.getTraceId());
      addMeasurements(execution);
   }

   private void addMeasurements(final Execution execution) {
      final String kiekerPattern = KiekerPatternConverter.getKiekerPattern(execution.getOperation());
      EOIESSIndex lookupindex = new EOIESSIndex(execution.getEss(), execution.getEoi(), kiekerPattern);
      CallTreeNode node = measuredNodes.get(lookupindex);
      if (node != null) {
         // Get duration in mikroseconds - Kieker produces nanoseconds
         final long duration = (execution.getTout() - execution.getTin());
         node.addMeasurement(version, duration);
      } else {
         LOG.error("Did not find {} Eoi: {} ESS: {} ", kiekerPattern, execution.getEoi(), execution.getEss());
      }
   }
}
