package de.dagere.peass.dependency.analysis;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import teetime.framework.Execution;

public class KiekerReader {

   private static final Logger LOG = LogManager.getLogger(KiekerReader.class);
   
   public static CalledMethodStage getCalledMethodStage(final File kiekerTraceFolder, final String prefix, final ModuleClassMapping mapping) {
      KiekerReaderConfiguration configuration = new KiekerReaderConfiguration();
      CalledMethodStage peassStage = configuration.exampleReader(kiekerTraceFolder, prefix, mapping);
      
      Execution execution = new Execution(configuration);
      execution.executeBlocking();
      LOG.debug("Execution finished succesfully");

      return peassStage;
   }
}
