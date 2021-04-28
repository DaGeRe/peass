package de.dagere.peass.measurement.rca.kiekerReading;

import java.io.File;
import java.util.Set;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.analysis.KiekerReaderConfiguration;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kieker.TreeStage;
import kieker.analysis.trace.execution.ExecutionRecordTransformationStage;
import kieker.analysis.trace.reconstruction.TraceReconstructionStage;

public class KiekerReaderConfigurationDuration extends KiekerReaderConfiguration {
   public void readDurations(final File kiekerTraceFolder, final Set<CallTreeNode> measuredNodes, final String version) {
      DurationStage stage = new DurationStage(systemModelRepositoryNew, measuredNodes, version);
      
      ExecutionRecordTransformationStage executionStage = prepareTillExecutions(kiekerTraceFolder);
      this.connectPorts(executionStage.getOutputPort(), stage.getInputPort());
   }
   
   public TreeStage readTree(final File kiekerTraceFolder, final TestCase test, final boolean ignoreEOIs, final MeasurementConfiguration config, final ModuleClassMapping mapping) {
      TreeStage treeStage = new TreeStage(systemModelRepositoryNew, test, ignoreEOIs, config, mapping);
      
      TraceReconstructionStage executionStage = prepareTillExecutionTrace(kiekerTraceFolder);
      this.connectPorts(executionStage.getExecutionTraceOutputPort(), treeStage.getInputPort());
      
      return treeStage;
   }
}
