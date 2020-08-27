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
import de.peass.measurement.rca.CauseTester;
import de.peass.measurement.rca.kieker.BothTreeReader;
import de.peass.testtransformation.JUnitTestTransformer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Searches for root cause of a performance change, i.e. method causing the performance change", name = "searchcause")
public class RootCauseAnalysis extends DependencyTestStarter {
   
   private static final Logger LOG = LogManager.getLogger(RootCauseAnalysis.class);

   @Option(names = { "-measureComplete", "--measureComplete" }, description = "Whether to measure the whole tree at once (default false - tree is measured level-wise)")
   public boolean measureComplete = false;

   @Option(names = { "-useCalibrationRun", "--useCalibrationRun" }, description = "Use the calibration run for complete measurements")
   public boolean useCalibrationRun = false;

   @Option(names = { "-useNonAggregatedWriter",
         "--useNonAggregatedWriter" }, description = "Whether to save non-aggregated JSON data for measurement results - if true, full kieker record data are stored")
   public boolean useNonAggregatedWriter = false;

   @Option(names = { "-saveKieker", "--saveKieker" }, description = "Save no kieker results in order to use less space - default false")
   public boolean saveNothing = false;
   
   @Option(names = { "-ignoreEOIs", "--ignoreEOIs" }, description = "Ignore EOIs - nodes will only be considered different if their kieker pattern or ess differ (saves space and computation time for big trees)")
   public boolean ignoreEOIs = false;

   @Option(names = { "-notSplitAggregated", "--notSplitAggregated" }, description = "Whether to split the aggregated data (produces aggregated data per time slice)")
   public boolean notSplitAggregated = false;

   @Option(names = { "-outlierFactor", "--outlierFactor" }, description = "Whether outliers should be removed with z-score higher than the given value")
   public double outlierFactor = 5.0;

   @Option(names = { "-minTime",
         "--minTime" }, description = "Minimum node difference time compared to relative standard deviation. "
               + "If a node takes less time, its childs won't be measured (since time measurement isn't below accurate below a certain value).")
   public double minTime = 0.1;

   @Option(names = { "-writeInterval", "--writeInterval" }, description = "Interval for KoPeMe-aggregated-writing (in milliseconds)")
   public int writeInterval = 5000;

   public static void main(final String[] args) throws JAXBException, IOException {
      final RootCauseAnalysis command = new RootCauseAnalysis();
      final CommandLine commandLine = new CommandLine(command);
      commandLine.execute(args);
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

      LOG.debug("Timeout in minutes: {}", timeout);
      final MeasurementConfiguration measurementConfiguration = new MeasurementConfiguration(timeout, vms, type1error, type2error, earlyStop, version, predecessor);
      measurementConfiguration.setWarmup(warmup);
      measurementConfiguration.setIterations(iterations);
      measurementConfiguration.setRepetitions(repetitions);
      measurementConfiguration.setUseKieker(true);
      measurementConfiguration.setUseGC(useGC);
      measurementConfiguration.setKiekerAggregationInterval(writeInterval);
      final JUnitTestTransformer testtransformer = getTestTransformer(measurementConfiguration);

      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(test, !useNonAggregatedWriter, !saveNothing,
            outlierFactor, !notSplitAggregated, minTime, useCalibrationRun, ignoreEOIs);
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
