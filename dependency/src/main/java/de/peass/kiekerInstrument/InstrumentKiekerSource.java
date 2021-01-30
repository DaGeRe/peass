package de.peass.kiekerInstrument;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.execution.AllowedKiekerRecord;

/**
 * Adds kieker monitoring code to existing source code *in-place*, i.e. the existing .java-files will get changed.
 * 
 * @author reichelt
 *
 */
public class InstrumentKiekerSource {

   private static final Logger LOG = LogManager.getLogger(InstrumentKiekerSource.class);

   private InstrumentationConfiguration configuration;

   private final BlockBuilder blockBuilder;

   public InstrumentKiekerSource(AllowedKiekerRecord usedRecord) {
      configuration = new InstrumentationConfiguration(usedRecord, false, true, true, new HashSet<>());
      configuration.getIncludedPatterns().add("*");
      this.blockBuilder = configuration.isSample() ? new SamplingBlockBuilder(configuration.getUsedRecord()) : new BlockBuilder(configuration.getUsedRecord(), true);
   }

   public InstrumentKiekerSource(InstrumentationConfiguration configuration) {
      this.configuration = configuration;
      this.blockBuilder = configuration.isSample() ? new SamplingBlockBuilder(configuration.getUsedRecord()) : new BlockBuilder(configuration.getUsedRecord(), false);
   }

   public void instrumentProject(File projectFolder) throws IOException {
      for (File javaFile : FileUtils.listFiles(projectFolder, new WildcardFileFilter("*.java"), TrueFileFilter.INSTANCE)) {
         LOG.debug("Instrumenting: " + javaFile);
         instrument(javaFile);
      }
   }

   public void instrument(File file) throws IOException {
      FileInstrumenter instrumenter = new FileInstrumenter(file, configuration, blockBuilder);
      instrumenter.instrument();
   }

}
