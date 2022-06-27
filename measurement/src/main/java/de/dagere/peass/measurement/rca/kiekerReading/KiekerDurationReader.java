package de.dagere.peass.measurement.rca.kiekerReading;

import java.io.File;
import java.util.List;
import java.util.Set;

import de.dagere.kopeme.kopemedata.MeasuredValue;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kieker.TreeStage;
import teetime.framework.Execution;

public class KiekerDurationReader {

   public static void executeDurationStage(final File kiekerTraceFolder, final Set<CallTreeNode> measuredNodes, final String version) {
      KiekerReaderConfigurationDuration configuration = new KiekerReaderConfigurationDuration();
      configuration.readDurations(kiekerTraceFolder, measuredNodes, version);
      
      Execution execution = new Execution(configuration);
      execution.executeBlocking();
   }
   
   public static void executeReducedDurationStage(final File kiekerTraceFolder, final Set<CallTreeNode> measuredNodes, final String version) {
      KiekerReaderConfigurationDuration configuration = new KiekerReaderConfigurationDuration();
      configuration.readReducedDurations(kiekerTraceFolder, measuredNodes, version);
      
      Execution execution = new Execution(configuration);
      execution.executeBlocking();
   }
   
   public static List<MeasuredValue> executeReducedDurationStage(final File kiekerTraceFolder) {
      KiekerReaderConfigurationDuration configuration = new KiekerReaderConfigurationDuration();
      DurationMeasurementStage stage = configuration.readReducedDurationsToList(kiekerTraceFolder);
      
      Execution execution = new Execution(configuration);
      execution.executeBlocking();
      
      return stage.getValues();
   }
   
   public static TreeStage executeTreeStage(final File kiekerTraceFolder, final TestCase test, 
         final boolean ignoreEOIs, final MeasurementConfig config, final ModuleClassMapping mapping) {
      KiekerReaderConfigurationDuration configuration = new KiekerReaderConfigurationDuration();
      TreeStage stage = configuration.readTree(kiekerTraceFolder,  test, ignoreEOIs,  config, mapping);
      
      Execution execution = new Execution(configuration);
      execution.executeBlocking();
      
      return stage;
   }

}
