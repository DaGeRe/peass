package de.peass;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependency.persistence.Version;
import de.peass.measurement.rca.CauseSearcher;
import de.peass.measurement.rca.CauseSearcherComplete;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.CauseSearcherConfigMixin;
import de.peass.measurement.rca.CauseTester;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.testtransformation.JUnitTestTransformer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(description = "Searches for root cause of a performance change, i.e. method causing the performance change", name = "searchcause")
public class RootCauseAnalysis extends DependencyTestStarter {

   enum MeasurementModes {
      COMPLETE, LEVEL, PARTIALTREES
   }

   private static final Logger LOG = LogManager.getLogger(RootCauseAnalysis.class);

   @Option(names = { "-measureComplete", "--measureComplete" }, description = "Whether to measure the whole tree at once (default false - tree is measured level-wise)")
   public boolean measureComplete = false;

   @Mixin
   private CauseSearcherConfigMixin causeSearchConfigMixin;

   @Option(names = { "-writeInterval", "--writeInterval" }, description = "Interval for KoPeMe-aggregated-writing (in milliseconds)")
   public int writeInterval = 5000;

   public static void main(final String[] args) throws JAXBException, IOException {
      final RootCauseAnalysis command = new RootCauseAnalysis();
      final CommandLine commandLine = new CommandLine(command);
      System.exit(commandLine.execute(args));
   }

   public RootCauseAnalysis() throws JAXBException, IOException {
      super();
   }

   @Override
   public Void call() throws Exception {
      if (testName == null) {
         throw new RuntimeException("Test needs to be defined!");
      }

      initVersionProcessor();

      if (version == null) {
         version = executionData.getVersions().keySet().iterator().next();
         LOG.info("Version was not defined, using " + version);
      }

      final TestCase test = new TestCase(testName);
      final Version versionInfo = dependencies.getVersions().get(version);
      final String predecessor = versionInfo.getPredecessor();

      LOG.debug("Timeout in minutes: {}", measurementConfigMixin.getTimeout());
      final MeasurementConfiguration measurementConfiguration = new MeasurementConfiguration(measurementConfigMixin);
      measurementConfiguration.setUseKieker(true);
      measurementConfiguration.setKiekerAggregationInterval(writeInterval);
      measurementConfiguration.setVersion(version);
      measurementConfiguration.setVersionOld(predecessor);
      final JUnitTestTransformer testtransformer = getTestTransformer(measurementConfiguration);

      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(test, causeSearchConfigMixin);
      final CauseSearchFolders alternateFolders = new CauseSearchFolders(folders.getProjectFolder());
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, measurementConfiguration, alternateFolders);
      if (measureComplete) {
         final CauseTester measurer = new CauseTester(alternateFolders, testtransformer, causeSearcherConfig);
         final CauseSearcher tester = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, measurementConfiguration, alternateFolders);
         tester.search();
      } else {
         final CauseTester measurer = new CauseTester(alternateFolders, testtransformer, causeSearcherConfig);
         final CauseSearcher tester = new CauseSearcher(reader, causeSearcherConfig, measurer, measurementConfiguration, alternateFolders);
         tester.search();
      }

      return null;
   }

}
