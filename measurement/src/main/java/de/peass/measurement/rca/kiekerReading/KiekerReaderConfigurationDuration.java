package de.peass.measurement.rca.kiekerReading;

import java.io.File;
import java.util.Set;

import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.analysis.KiekerReaderConfiguration;
import de.peass.dependency.analysis.ModuleClassMapping;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.kieker.TreeStage;
import kieker.analysis.trace.execution.ExecutionRecordTransformationStage;
import kieker.analysis.trace.reconstruction.TraceReconstructionStage;

public class KiekerReaderConfigurationDuration extends KiekerReaderConfiguration {
   public void readDurations(final File kiekerTraceFolder, final Set<CallTreeNode> measuredNodes, final String version) {
      DurationStage stage = new DurationStage(systemModelRepositoryNew, measuredNodes, version);
      
      ExecutionRecordTransformationStage executionStage = prepareTillExecutions(kiekerTraceFolder);
      this.connectPorts(executionStage.getOutputPort(), stage.getInputPort());
   }
   
   public TreeStage readTree(final File kiekerTraceFolder, final String prefix, final TestCase test, final boolean ignoreEOIs, final MeasurementConfiguration config, final ModuleClassMapping mapping) {
      TreeStage treeStage = new TreeStage(systemModelRepositoryNew, prefix, test, ignoreEOIs, config, mapping);
      
      TraceReconstructionStage executionStage = prepareTillExecutionTrace(kiekerTraceFolder);
      this.connectPorts(executionStage.getExecutionTraceOutputPort(), treeStage.getInputPort());
      
      return treeStage;
   }
}
