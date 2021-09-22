package net.kieker.sourceinstrumentation.instrument;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.kieker.sourceinstrumentation.AllowedKiekerRecord;
import net.kieker.sourceinstrumentation.InstrumentationConfiguration;

/**
 * Adds kieker monitoring code to existing source code *in-place*, i.e. the existing .java-files will get changed.
 * 
 * @author reichelt
 *
 */
public class InstrumentKiekerSource {

   private static final Logger LOG = LogManager.getLogger(InstrumentKiekerSource.class);

   private InstrumentationConfiguration configuration;

   public InstrumentKiekerSource(final AllowedKiekerRecord usedRecord) {
      configuration = new InstrumentationConfiguration(usedRecord, false, true, true, new HashSet<>(), false, 1000, false);
      configuration.getIncludedPatterns().add("*");
   }

   public InstrumentKiekerSource(final InstrumentationConfiguration configuration) {
      this.configuration = configuration;
   }

   public void instrumentProject(final File projectFolder) throws IOException {
      for (File javaFile : FileUtils.listFiles(projectFolder, new WildcardFileFilter("*.java"), TrueFileFilter.INSTANCE)) {
         LOG.debug("Instrumenting: " + javaFile);
         instrument(javaFile);
      }
   }

   public void instrument(final File file) throws IOException {
      FileInstrumenter instrumenter = new FileInstrumenter(file, configuration);
      instrumenter.instrument();
   }

}
