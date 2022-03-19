package de.dagere.peass.debugtools;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.dagere.peass.dependency.analysis.data.ChangedEntityHelper;
import kieker.analysis.stage.DynamicEventDispatcher;
import kieker.analysis.stage.IEventMatcher;
import kieker.analysis.stage.ImplementsEventMatcher;
import kieker.analysis.trace.AbstractTraceProcessingStage;
import kieker.analysis.trace.execution.ExecutionRecordTransformationStage;
import kieker.common.record.controlflow.OperationExecutionRecord;
import kieker.model.repository.SystemModelRepository;
import kieker.model.system.model.Execution;
import kieker.tools.source.LogsReaderCompositeStage;
import teetime.framework.Configuration;

class CallCountStage extends AbstractTraceProcessingStage<Execution> {

   Map<String, Integer> signatureCounts = new HashMap<>();

   public CallCountStage(SystemModelRepository systemModelRepository) {
      super(systemModelRepository);
   }

   @Override
   protected void execute(Execution execution) throws Exception {
      final String fullClassname = execution.getOperation().getComponentType().getFullQualifiedName().intern();
      final String methodname = execution.getOperation().getSignature().getName().intern();
      String methodWithParameters = methodname + ChangedEntityHelper.getParameterString(execution.getOperation().getSignature().getParamTypeList());
      String signature = fullClassname + "." + methodWithParameters;

      if (!signatureCounts.containsKey(signature)) {
         signatureCounts.put(signature, 0);
      }

      int oldCount = signatureCounts.get(signature);
      signatureCounts.put(signature, oldCount + 1);
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

      teetime.framework.Execution<?> execution = new teetime.framework.Execution(configuration);
      execution.executeBlocking();

      System.out.println("Signatures: " + stage.signatureCounts.size());

      stage.signatureCounts.entrySet()
            .stream()
            .sorted(new Comparator<Entry<String, Integer>>() {
               @Override
               public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
                  return o2.getValue() - o1.getValue();
               }
            }).forEach(entry -> {
               System.out.println("Signature: " + entry.getKey() + " count: " + entry.getValue());
            });

   }
}
