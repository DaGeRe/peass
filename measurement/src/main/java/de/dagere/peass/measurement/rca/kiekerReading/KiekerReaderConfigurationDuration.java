package de.dagere.peass.measurement.rca.kiekerReading;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.dagere.kopeme.kieker.record.DurationRecord;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.KiekerReaderConfiguration;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kieker.TreeStage;
import kieker.analysis.architecture.trace.execution.ExecutionRecordTransformationStage;
import kieker.analysis.architecture.trace.reconstruction.TraceReconstructionStage;
import kieker.analysis.generic.DynamicEventDispatcher;
import kieker.analysis.generic.IEventMatcher;
import kieker.analysis.generic.ImplementsEventMatcher;
import kieker.tools.source.LogsReaderCompositeStage;

public class KiekerReaderConfigurationDuration extends KiekerReaderConfiguration {
   
   public void readUSCDurations(final File kiekerTraceFolder, final Set<CallTreeNode> measuredNodes, final String commit) {
      OperationExecutionUSCStage stage = new OperationExecutionUSCStage(systemModelRepositoryNew, measuredNodes, commit);
      
      ExecutionRecordTransformationStage executionStage = prepareTillExecutions(kiekerTraceFolder);
      this.connectPorts(executionStage.getOutputPort(), stage.getInputPort());
   }
   
   public void readDurations(final File kiekerTraceFolder, final Set<CallTreeNode> measuredNodes, final String commit) {
      OperationExecutionRCAStage stage = new OperationExecutionRCAStage(systemModelRepositoryNew, measuredNodes, commit);
      
      ExecutionRecordTransformationStage executionStage = prepareTillExecutions(kiekerTraceFolder);
      this.connectPorts(executionStage.getOutputPort(), stage.getInputPort());
   }
   
   protected void readReducedDurations(final File kiekerTraceFolder, final Set<CallTreeNode> measuredNodes, final String commit) {
      List<File> inputDirs = new LinkedList<File>();
      inputDirs.add(kiekerTraceFolder);
      LogsReaderCompositeStage logReaderStage = new LogsReaderCompositeStage(inputDirs, true, 4096);

      final DurationRCAStage executionRecordTransformationStage = new DurationRCAStage(systemModelRepositoryNew, measuredNodes, commit);

      final DynamicEventDispatcher dispatcher = new DynamicEventDispatcher(null, false, true, false);
      final IEventMatcher<? extends DurationRecord> operationExecutionRecordMatcher = new ImplementsEventMatcher<>(DurationRecord.class, null);
      dispatcher.registerOutput(operationExecutionRecordMatcher);

      this.connectPorts(logReaderStage.getOutputPort(), dispatcher.getInputPort());
      this.connectPorts(operationExecutionRecordMatcher.getOutputPort(), executionRecordTransformationStage.getInputPort());
   }
   
   protected DurationMeasurementStage readReducedDurationsToList(final File kiekerTraceFolder) {
      List<File> inputDirs = new LinkedList<File>();
      inputDirs.add(kiekerTraceFolder);
      LogsReaderCompositeStage logReaderStage = new LogsReaderCompositeStage(inputDirs, true, 4096);

      final DurationMeasurementStage executionRecordTransformationStage = new DurationMeasurementStage(systemModelRepositoryNew);

      final DynamicEventDispatcher dispatcher = new DynamicEventDispatcher(null, false, true, false);
      final IEventMatcher<? extends DurationRecord> operationExecutionRecordMatcher = new ImplementsEventMatcher<>(DurationRecord.class, null);
      dispatcher.registerOutput(operationExecutionRecordMatcher);

      this.connectPorts(logReaderStage.getOutputPort(), dispatcher.getInputPort());
      this.connectPorts(operationExecutionRecordMatcher.getOutputPort(), executionRecordTransformationStage.getInputPort());
      
      return executionRecordTransformationStage;
   }
   
   public TreeStage readTree(final File kiekerTraceFolder, final TestMethodCall test, final boolean ignoreEOIs, final MeasurementConfig config, final ModuleClassMapping mapping) {
      TreeStage treeStage = new TreeStage(systemModelRepositoryNew, test, ignoreEOIs, config, mapping);
      
      TraceReconstructionStage executionStage = prepareTillExecutionTrace(kiekerTraceFolder);
      this.connectPorts(executionStage.getExecutionTraceOutputPort(), treeStage.getInputPort());
      
      return treeStage;
   }
}
