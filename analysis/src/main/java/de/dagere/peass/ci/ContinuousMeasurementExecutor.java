package de.dagere.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.ci.logHandling.LogRedirector;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.dependencyprocessors.AdaptiveTester;

public class ContinuousMeasurementExecutor {

   private static final Logger LOG = LogManager.getLogger(ContinuousMeasurementExecutor.class);

   private final PeassFolders folders;
   private final MeasurementConfig measurementConfig;
   private final EnvironmentVariables env;
   private final CommitComparatorInstance comparator;

   public ContinuousMeasurementExecutor(final PeassFolders folders, final MeasurementConfig measurementConfig,
         final EnvironmentVariables env, CommitComparatorInstance comparator) {
      this.folders = folders;
      this.measurementConfig = measurementConfig;
      this.env = env;
      this.comparator = comparator;
   }

   public File executeMeasurements(final Set<TestMethodCall> tests, final File fullResultsVersion, final File logFile) throws IOException {
      if (!fullResultsVersion.exists()) {
         if (measurementConfig.getExecutionConfig().isRedirectSubprocessOutputToFile()) {
            LOG.info("Executing measurement - Log goes to {}", logFile.getAbsolutePath());
            try (LogRedirector director = new LogRedirector(logFile)) {
               doMeasurement(tests, fullResultsVersion);
            }
         } else {
            doMeasurement(tests, fullResultsVersion);
         }

      } else {
         LOG.info("Skipping measurement - result folder {} already existing", fullResultsVersion.getAbsolutePath());
      }
      final File measurementFolder = new File(fullResultsVersion, PeassFolders.MEASUREMENTS);
      return measurementFolder;
   }

   private void doMeasurement(final Set<TestMethodCall> tests, final File fullResultsVersion) throws IOException {
      cleanTemporaryFolders();

      for (final TestMethodCall test : tests) {
         MeasurementConfig copied = createCopiedConfiguration();
         final AdaptiveTester tester = new AdaptiveTester(folders, copied, env, comparator);
         tester.evaluate(test);
      }

      final File fullResultsFolder = folders.getFullMeasurementFolder();
      LOG.debug("Moving to: {}", fullResultsVersion.getAbsolutePath());
      FileUtils.moveDirectory(fullResultsFolder, fullResultsVersion);
   }

   private void cleanTemporaryFolders() throws IOException {
      final File fullResultsFolder = folders.getFullMeasurementFolder();
      FileUtils.deleteDirectory(fullResultsFolder);
      fullResultsFolder.mkdirs();
      folders.getDetailResultFolder().mkdirs();
      FileUtils.deleteDirectory(folders.getTempMeasurementFolder());
      folders.getTempMeasurementFolder().mkdirs();
   }

   private MeasurementConfig createCopiedConfiguration() {
      MeasurementConfig copied = new MeasurementConfig(measurementConfig);
      copied.getKiekerConfig().setUseKieker(false);
      return copied;
   }
}
