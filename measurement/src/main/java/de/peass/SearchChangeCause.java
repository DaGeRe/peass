package de.peass;

import java.io.IOException;

import javax.xml.bind.JAXBException;

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
public class SearchChangeCause extends AdaptiveTestStarter {

   @Option(names = { "-measureComplete", "--measureComplete" }, description = "Whether to measure the whole tree at once (default false - tree is measured level-wise)")
   public boolean measureComplete = false;

   @Option(names = { "-skipCalibrationRun", "--skipCalibrationRun" }, description = "Skip the calibration run for complete measurements")
   public boolean skipCalibrationRun = false;

   @Option(names = { "-useNonAggregatedWriter",
         "--useNonAggregatedWriter" }, description = "Whether to save non-aggregated JSON data for measurement results - if true, full kieker record data are stored")
   public boolean useNonAggregatedWriter = false;

   @Option(names = { "-saveKieker", "--saveKieker" }, description = "Save no kieker results in order to use less space - default false")
   public boolean saveNothing = false;

   @Option(names = { "-notSplitAggregated", "--notSplitAggregated" }, description = "Whether to split the aggregated data (produces aggregated data per time slice)")
   public boolean notSplitAggregated = false;

   @Option(names = { "-outlierFactor", "--outlierFactor" }, description = "Whether outliers should be removed with z-score higher than the given value")
   public double outlierFactor = 5.0;

   @Option(names = { "-minTime",
         "--minTime" }, description = "Minimum time for a method call to be processed. If it takes less time, it won't be measured (since time measurement isn't below accurate below a certain value).")
   public double minTime = 1.0;

   @Option(names = { "-writeInterval", "--writeInterval" }, description = "Interval for KoPeMe-aggregated-writing (in milliseconds)")
   public int writeInterval = 5000;

   public static void main(final String[] args) throws JAXBException, IOException {
      final SearchChangeCause command = new SearchChangeCause();
      final CommandLine commandLine = new CommandLine(command);
      commandLine.execute(args);
   }

   public SearchChangeCause() throws JAXBException, IOException {
      super();
   }

   @Override
   public Void call() throws Exception {
      initVersionProcessor();

      final TestCase test = new TestCase(testName);
      final Version versionInfo = dependencies.getVersions().get(version);
      final String predecessor = versionInfo.getPredecessor();

      final MeasurementConfiguration measurementConfiguration = new MeasurementConfiguration(timeout * 1000 * 60, vms, type1error, type2error, version, predecessor);
      measurementConfiguration.setWarmup(warmup);
      measurementConfiguration.setIterations(iterations);
      measurementConfiguration.setRepetitions(repetitions);
      measurementConfiguration.setUseKieker(true);
      measurementConfiguration.setKiekerAggregationInterval(writeInterval);
      final JUnitTestTransformer testtransformer = getTestTransformer(measurementConfiguration);

      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(test, !useNonAggregatedWriter, !saveNothing,
            outlierFactor, !notSplitAggregated, minTime, !skipCalibrationRun);
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
