package de.peass.measurement.rca.kiekerReading;

import java.io.File;
import java.util.Set;

import de.peass.dependency.analysis.KiekerReaderConfiguration;
import de.peass.measurement.rca.data.CallTreeNode;
import kieker.analysis.trace.execution.ExecutionRecordTransformationStage;

public class KiekerReaderConfigurationDuration extends KiekerReaderConfiguration {
   public void readDurations(final File kiekerTraceFolder, final Set<CallTreeNode> measuredNodes, final String version) {
      DurationStage stage = new DurationStage(systemModelRepositoryNew, measuredNodes, version);
      
      ExecutionRecordTransformationStage executionStage = prepareTillExecutions(kiekerTraceFolder);
      this.connectPorts(executionStage.getOutputPort(), stage.getInputPort());
   }
}
