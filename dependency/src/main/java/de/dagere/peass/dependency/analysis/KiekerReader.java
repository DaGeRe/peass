package de.dagere.peass.dependency.analysis;

import java.io.File;

import teetime.framework.Execution;

public class KiekerReader {

   public static CalledMethodStage getCalledMethodStage(final File kiekerTraceFolder, final String prefix, final ModuleClassMapping mapping) {
      KiekerReaderConfiguration configuration = new KiekerReaderConfiguration();
      CalledMethodStage peassStage = configuration.exampleReader(kiekerTraceFolder, prefix, mapping);
      
      Execution execution = new Execution(configuration);
      execution.executeBlocking();

      return peassStage;
   }
}
