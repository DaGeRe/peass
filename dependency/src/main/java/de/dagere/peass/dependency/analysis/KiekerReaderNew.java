package de.dagere.peass.dependency.analysis;

import java.io.File;

import teetime.framework.Execution;

public class KiekerReaderNew {

   public static PeassStage getPeassStage(final File kiekerTraceFolder, final String prefix, final ModuleClassMapping mapping) {
      KiekerReaderConfiguration configuration = new KiekerReaderConfiguration();
      PeassStage peassStage = configuration.exampleReader(kiekerTraceFolder, prefix, mapping);
      
      Execution execution = new Execution(configuration);
      execution.executeBlocking();

      return peassStage;
   }
}
