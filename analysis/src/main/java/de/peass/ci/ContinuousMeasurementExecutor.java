package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependencyprocessors.AdaptiveTester;

public class ContinuousMeasurementExecutor {

   private static final Logger LOG = LogManager.getLogger(ContinuousMeasurementExecutor.class);

   private final String version, versionOld;
   private final PeASSFolders folders;
   private final MeasurementConfiguration measurementConfig;
   private final EnvironmentVariables env; 

   public ContinuousMeasurementExecutor(final String version, final String versionOld, final PeASSFolders folders, final MeasurementConfiguration measurementConfig, final EnvironmentVariables env) {
      this.version = version;
      this.versionOld = versionOld;
      this.folders = folders;
      this.measurementConfig = measurementConfig;
      this.env = env;
   }

   public File executeMeasurements(final Set<TestCase> tests, final File fullResultsVersion) throws IOException, InterruptedException, JAXBException, XmlPullParserException {
      if (!fullResultsVersion.exists()) {
         File logFile = new File(fullResultsVersion.getParentFile(), "measurement_" + version + "_" + versionOld + ".txt");
         LOG.info("Executing measurement - Log goes to {}", logFile.getAbsolutePath());
         try (LogRedirector director = new LogRedirector(logFile)) {
            MeasurementConfiguration copied = new MeasurementConfiguration(measurementConfig);
            copied.setUseKieker(false);
            copied.setVersion(version);
            copied.setVersionOld(versionOld);

            final AdaptiveTester tester = new AdaptiveTester(folders, copied, env);
            for (final TestCase test : tests) {
               tester.evaluate(test);
            }

            final File fullResultsFolder = folders.getFullMeasurementFolder();
            LOG.debug("Moving to: {}", fullResultsVersion.getAbsolutePath());
            FileUtils.moveDirectory(fullResultsFolder, fullResultsVersion);
         }
      } else {
         LOG.info("Skipping measurement - result folder {} already existing", fullResultsVersion.getAbsolutePath());
      }
      final File measurementFolder = new File(fullResultsVersion, "measurements");
      return measurementFolder;
   }
}
