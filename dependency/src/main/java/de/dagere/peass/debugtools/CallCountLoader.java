package de.dagere.peass.debugtools;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.dagere.peass.dependency.kiekerTemp.LogsReaderCompositeStage;
import kieker.analysis.stage.DynamicEventDispatcher;
import kieker.analysis.stage.IEventMatcher;
import kieker.analysis.stage.ImplementsEventMatcher;
import kieker.analysis.trace.AbstractTraceProcessingStage;
import kieker.analysis.trace.execution.ExecutionRecordTransformationStage;
import kieker.common.record.controlflow.OperationExecutionRecord;
import kieker.model.repository.SystemModelRepository;
import kieker.model.system.model.Execution;
import teetime.framework.Configuration;

class CallCountStage extends AbstractTraceProcessingStage<Execution> {

   Map<String, Integer> signatureCounts = new HashMap<>();
   
   public CallCountStage(SystemModelRepository systemModelRepository) {
      super(systemModelRepository);
   }

   @Override
   protected void execute(Execution element) throws Exception {
      String signature = element.getOperation().getSignature().toString();
      
      if (!signatureCounts.containsKey(signature)) {
         signatureCounts.put(signature, 0);
      }
      
      int oldCount = signatureCounts.get(signature);
      signatureCounts.put(signature, oldCount+1);
   }

}

class CallCountConfiguration extends Configuration {

   protected SystemModelRepository systemModelRepositoryNew = new SystemModelRepository();

   public CallCountStage prepareCallCount(File kiekerTraceFolder) {
      final ExecutionRecordTransformationStage executionRecordTransformationStage = prepareTillExecutions(kiekerTraceFolder);
      
      CallCountStage callCountStage = new CallCountStage(systemModelRepositoryNew);
      this.connectPorts(executionRecordTransformationStage.getOutputPort(), callCountStage.getInputPort());
      return callCountStage;
   }
   
   protected ExecutionRecordTransformationStage prepareTillExecutions(final File kiekerTraceFolder) {
      List<File> inputDirs = new LinkedList<File>();
      inputDirs.add(kiekerTraceFolder);
      LogsReaderCompositeStage logReaderStage = new LogsReaderCompositeStage(inputDirs, true, 4096);

      final ExecutionRecordTransformationStage executionRecordTransformationStage = new ExecutionRecordTransformationStage(systemModelRepositoryNew);

      final DynamicEventDispatcher dispatcher = new DynamicEventDispatcher(null, false, true, false);
      final IEventMatcher<? extends OperationExecutionRecord> operationExecutionRecordMatcher = new ImplementsEventMatcher<>(OperationExecutionRecord.class, null);
      dispatcher.registerOutput(operationExecutionRecordMatcher);

      this.connectPorts(logReaderStage.getOutputPort(), dispatcher.getInputPort());
      this.connectPorts(operationExecutionRecordMatcher.getOutputPort(), executionRecordTransformationStage.getInputPort());
      return executionRecordTransformationStage;
   }
}

public class CallCountLoader {
   public static void main(String[] args) {
      File loadedFile = new File(args[0]);
      if (!loadedFile.isDirectory()) {
         throw new RuntimeException("File was no directory: " + loadedFile);
      }

      CallCountConfiguration configuration = new CallCountConfiguration();
      CallCountStage stage = configuration.prepareCallCount(loadedFile);
      
      teetime.framework.Execution execution = new teetime.framework.Execution(configuration);
      execution.executeBlocking();
      
      System.out.println("Signatures: " + stage.signatureCounts.size());
      
      stage.signatureCounts.forEach((signature, count) -> {
         System.out.println("Signature: " + signature + " count: " + count);
      });
   }
}
