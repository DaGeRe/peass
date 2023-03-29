package de.dagere.peass.ci;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.analysis.properties.PropertyReader;
import de.dagere.peass.ci.logHandling.LogRedirector;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;

public class SourceReader {
   private static final Logger LOG = LogManager.getLogger(SourceReader.class);

   private final String commit, commitOld;
   private final ResultsFolders resultsFolders;
   private final PeassFolders folders;
   private final ExecutionConfig config;

   public SourceReader(final ExecutionConfig config, final String commit, final String commitOld, final ResultsFolders resultsFolders, final PeassFolders folders) {
      this.commit = commit;
      this.commitOld = commitOld;
      this.resultsFolders = resultsFolders;
      this.folders = folders;
      this.config = config;
   }

   public void readMethodSources(final Set<TestMethodCall> tests) {
      if (config.isRedirectSubprocessOutputToFile()) {
         File logFile = resultsFolders.getSourceReadLogFile(commit, commitOld);
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

   public void executeSourceReading(final Set<TestMethodCall> tests) {
      ExecutionData executionData = new ExecutionData();
      executionData.addEmptyCommit(commit, commitOld);
      if (commitOld != null) {
         executionData.addEmptyCommit(commitOld, null);
      }
      for (TestMethodCall test : tests) {
         executionData.addCall(commit, test);
      }
      LOG.info("Reading method sources for {} - {}", commit, commitOld);
      final PropertyReader propertyReader = new PropertyReader(resultsFolders, folders.getProjectFolder(), executionData, config);
      propertyReader.readAllTestsProperties();
   }
}
