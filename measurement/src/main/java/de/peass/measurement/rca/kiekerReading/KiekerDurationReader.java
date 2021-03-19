package de.peass.measurement.rca.kiekerReading;

import java.io.File;
import java.util.Set;

import de.peass.measurement.rca.data.CallTreeNode;
import teetime.framework.Execution;

public class KiekerDurationReader {

   public static void executeDurationStage(final File kiekerTraceFolder, final Set<CallTreeNode> measuredNodes, final String version) {
      KiekerReaderConfigurationDuration configuration = new KiekerReaderConfigurationDuration();
      configuration.readDurations(kiekerTraceFolder, measuredNodes, version);
      
      Execution execution = new Execution(configuration);
      execution.executeBlocking();
   }

}
