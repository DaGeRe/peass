package de.peass.ci;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependencyprocessors.AdaptiveTester;
import de.peass.testtransformation.JUnitTestTransformer;

public class ContinuousMeasurementExecutor {
   
   private static final Logger LOG = LogManager.getLogger(ContinuousMeasurementExecutor.class);
   
   private final String version, versionOld;
   private final PeASSFolders folders;
   private final MeasurementConfiguration measurementConfig;
   
   public ContinuousMeasurementExecutor(String version, String versionOld, PeASSFolders folders, MeasurementConfiguration measurementConfig) {
      this.version = version;
      this.versionOld = versionOld;
      this.folders = folders;
      this.measurementConfig = measurementConfig;
   }

   public File executeMeasurements(final Set<TestCase> tests, final File fullResultsVersion) throws IOException, InterruptedException, JAXBException {
      if (!fullResultsVersion.exists()) {
         MeasurementConfiguration copied = new MeasurementConfiguration(measurementConfig);
         final JUnitTestTransformer testgenerator = new JUnitTestTransformer(folders.getProjectFolder(), copied);
         testgenerator.getConfig().setUseKieker(false);
         copied.setVersion(version);
         copied.setVersionOld(versionOld);

         final AdaptiveTester tester = new AdaptiveTester(folders, testgenerator);
         for (final TestCase test : tests) {
            tester.evaluate(test);
         }

         final File fullResultsFolder = folders.getFullMeasurementFolder();
         LOG.debug("Moving to: {}", fullResultsVersion.getAbsolutePath());
         FileUtils.moveDirectory(fullResultsFolder, fullResultsVersion);
      }

      final File measurementFolder = new File(fullResultsVersion, "measurements");
      return measurementFolder;
   }
}
