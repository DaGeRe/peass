package de.dagere.peass.ci;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.analysis.properties.PropertyReader;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;

public class SourceReader {
   private static final Logger LOG = LogManager.getLogger(SourceReader.class);

   private final String version, versionOld;
   private final ResultsFolders resultsFolders;
   private final PeassFolders folders;
   private final ExecutionConfig config;

   public SourceReader(final ExecutionConfig config, final String version, final String versionOld, final ResultsFolders resultsFolders, final PeassFolders folders) {
      this.version = version;
      this.versionOld = versionOld;
      this.resultsFolders = resultsFolders;
      this.folders = folders;
      this.config = config;
   }

   public void readMethodSources(final Set<TestCase> tests) {
      if (config.isRedirectSubprocessOutputToFile()) {
         File logFile = resultsFolders.getSourceReadLogFile(version, versionOld);
         LOG.info("Executing source reading - log goes to {}", logFile.getAbsolutePath());

         try (LogRedirector director = new LogRedirector(logFile)) {
            executeSourceReading(tests);
         } catch (FileNotFoundException e) {
            e.printStackTrace();
            executeSourceReading(tests);
         }
      } else {
         executeSourceReading(tests);
      }
   }

   public void executeSourceReading(final Set<TestCase> tests) {
      ExecutionData executionData = new ExecutionData();
      executionData.addEmptyVersion(version, versionOld);
      if (versionOld != null) {
         executionData.addEmptyVersion(versionOld, null);
      }
      for (TestCase test : tests) {
         executionData.addCall(version, test);
      }
      LOG.info("Reading method sources for {} - {}", version, versionOld);
      final PropertyReader propertyReader = new PropertyReader(resultsFolders, folders.getProjectFolder(), executionData);
      propertyReader.readAllTestsProperties();
   }
}
