package de.peass;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.persistence.Version;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.searchcause.CauseSearcher;
import de.peass.measurement.searchcause.CauseSearcherComplete;
import de.peass.measurement.searchcause.CauseSearcherConfig;
import de.peass.measurement.searchcause.LevelMeasurer;
import de.peass.measurement.searchcause.kieker.BothTreeReader;
import de.peass.testtransformation.JUnitTestTransformer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(description = "Searches for root cause of a performance change, i.e. method causing the performance change", name = "searchcause")
public class SearchChangeCause extends AdaptiveTestStarter {

   @Option(names = { "-useNonAggregatedWriter", "--useNonAggregatedWriter" }, description = "Whether to save non-aggregated JSON data for measurement results - if true, full kieker record data are stored")
   public boolean useNonAggregatedWriter = false;
   
   @Option(names = { "-saveAll", "--saveAll" }, description = "Whether to save all results (requires a lot of disc space)")
   public boolean saveAll = false;
   
   @Option(names = { "-outlierFactor", "--outlierFactor" }, description = "Whether outliers should be removed with z-score higher than the given value")
   public double outlierFactor = 5.0;
   
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

      final MeasurementConfiguration measurementConfiguration = new MeasurementConfiguration(timeout, vms, type1error, type2error, version, predecessor);
      measurementConfiguration.setWarmup(warmup);
      measurementConfiguration.setIterations(iterations);
      measurementConfiguration.setRepetitions(repetitions);
      final JUnitTestTransformer testtransformer = getTestTransformer(measurementConfiguration);

      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(test, !useNonAggregatedWriter, saveAll, outlierFactor);
      final CauseSearchFolders alternateFolders = new CauseSearchFolders(folders.getProjectFolder());
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, measurementConfiguration, alternateFolders);
      final boolean complete = false;
      if (complete) {
         final LevelMeasurer measurer = new LevelMeasurer(alternateFolders, causeSearcherConfig, testtransformer, measurementConfiguration);
         final CauseSearcher tester = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, measurementConfiguration, alternateFolders);
         tester.search();
      } else {
         final LevelMeasurer measurer = new LevelMeasurer(alternateFolders, causeSearcherConfig, testtransformer, measurementConfiguration);
         final CauseSearcher tester = new CauseSearcher(reader, causeSearcherConfig, measurer, measurementConfiguration, alternateFolders);
         tester.search();
      }

      return null;
   }

}
