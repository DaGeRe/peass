package de.dagere.peass.measurement.rca.kiekerReading;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.dagere.kopeme.kieker.record.DurationRecord;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.KiekerReaderConfiguration;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kieker.TreeStage;
import kieker.analysis.stage.DynamicEventDispatcher;
import kieker.analysis.stage.IEventMatcher;
import kieker.analysis.stage.ImplementsEventMatcher;
import kieker.analysis.trace.execution.ExecutionRecordTransformationStage;
import kieker.analysis.trace.reconstruction.TraceReconstructionStage;
import kieker.tools.source.LogsReaderCompositeStage;

public class KiekerReaderConfigurationDuration extends KiekerReaderConfiguration {
   public void readDurations(final File kiekerTraceFolder, final Set<CallTreeNode> measuredNodes, final String version) {
      OperationExecutionRCAStage stage = new OperationExecutionRCAStage(systemModelRepositoryNew, measuredNodes, version);
      
      ExecutionRecordTransformationStage executionStage = prepareTillExecutions(kiekerTraceFolder);
      this.connectPorts(executionStage.getOutputPort(), stage.getInputPort());
   }
   
   protected void readReducedDurations(final File kiekerTraceFolder, final Set<CallTreeNode> measuredNodes, final String version) {
      List<File> inputDirs = new LinkedList<File>();
      inputDirs.add(kiekerTraceFolder);
      LogsReaderCompositeStage logReaderStage = new LogsReaderCompositeStage(inputDirs, true, 4096);

      final DurationRCAStage executionRecordTransformationStage = new DurationRCAStage(systemModelRepositoryNew, measuredNodes, version);

      final DynamicEventDispatcher dispatcher = new DynamicEventDispatcher(null, false, true, false);
      final IEventMatcher<? extends DurationRecord> operationExecutionRecordMatcher = new ImplementsEventMatcher<>(DurationRecord.class, null);
      dispatcher.registerOutput(operationExecutionRecordMatcher);

      this.connectPorts(logReaderStage.getOutputPort(), dispatcher.getInputPort());
      this.connectPorts(operationExecutionRecordMatcher.getOutputPort(), executionRecordTransformationStage.getInputPort());
   }
   
   public TreeStage readTree(final File kiekerTraceFolder, final TestCase test, final boolean ignoreEOIs, final MeasurementConfig config, final ModuleClassMapping mapping) {
      TreeStage treeStage = new TreeStage(systemModelRepositoryNew, test, ignoreEOIs, config, mapping);
      
      TraceReconstructionStage executionStage = prepareTillExecutionTrace(kiekerTraceFolder);
      this.connectPorts(executionStage.getExecutionTraceOutputPort(), treeStage.getInputPort());
      
      return treeStage;
   }
}
