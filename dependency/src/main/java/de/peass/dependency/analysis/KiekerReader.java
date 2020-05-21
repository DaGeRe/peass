package de.peass.dependency.analysis;

import java.io.File;

import kieker.analysis.AnalysisController;
import kieker.analysis.exception.AnalysisConfigurationException;
import kieker.analysis.plugin.reader.filesystem.FSReader;
import kieker.common.configuration.Configuration;
import kieker.tools.trace.analysis.filter.AbstractTraceAnalysisFilter;
import kieker.tools.trace.analysis.filter.executionRecordTransformation.ExecutionRecordTransformationFilter;
import kieker.tools.trace.analysis.filter.traceReconstruction.TraceReconstructionFilter;
import kieker.tools.trace.analysis.systemModel.repository.SystemModelRepository;

public class KiekerReader {

   protected final AnalysisController analysisController = new AnalysisController();
   private final File kiekerTraceFolder;

   final SystemModelRepository systemModelRepository;

   final ExecutionRecordTransformationFilter executionRecordTransformationFilter;

   public KiekerReader(File kiekerTraceFolder) {
      this.kiekerTraceFolder = kiekerTraceFolder;

      systemModelRepository = new SystemModelRepository(new Configuration(), analysisController);
      executionRecordTransformationFilter = new ExecutionRecordTransformationFilter(new Configuration(),
            analysisController);
   }

   public void initBasic() throws IllegalStateException, AnalysisConfigurationException {
      final Configuration fsReaderConfig = new Configuration();
      fsReaderConfig.setProperty(FSReader.CONFIG_PROPERTY_NAME_INPUTDIRS, kiekerTraceFolder.getAbsolutePath());
      final FSReader reader = new FSReader(fsReaderConfig, analysisController);

      analysisController.connect(executionRecordTransformationFilter,
            AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemModelRepository);
      analysisController.connect(reader, FSReader.OUTPUT_PORT_NAME_RECORDS,
            executionRecordTransformationFilter, ExecutionRecordTransformationFilter.INPUT_PORT_NAME_RECORDS);
   }

   public TraceReconstructionFilter initTraceReconstruction()
         throws AnalysisConfigurationException {
      TraceReconstructionFilter traceReconstructionFilter = new TraceReconstructionFilter(new Configuration(), analysisController);
      analysisController.connect(traceReconstructionFilter,
            AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemModelRepository);
      analysisController.connect(executionRecordTransformationFilter, ExecutionRecordTransformationFilter.OUTPUT_PORT_NAME_EXECUTIONS,
            traceReconstructionFilter, TraceReconstructionFilter.INPUT_PORT_NAME_EXECUTIONS);
      return traceReconstructionFilter;
   }
   
   public AnalysisController getAnalysisController() {
      return analysisController;
   }

   public ExecutionRecordTransformationFilter getExecutionFilter() {
      return executionRecordTransformationFilter;
   }
}
