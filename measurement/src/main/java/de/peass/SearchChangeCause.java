package de.peass;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import de.dagere.kopeme.datacollection.DataCollectorList;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.searchcause.CauseSearcher;
import de.peass.measurement.searchcause.CauseSearcherComplete;
import de.peass.measurement.searchcause.CauseSearcherConfig;
import de.peass.measurement.searchcause.LevelMeasurer;
import de.peass.measurement.searchcause.kieker.BothTreeReader;
import de.peass.testtransformation.JUnitTestTransformer;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(description = "Searches for root cause of a performance change, i.e. method causing the performance change", name = "searchcause")
public class SearchChangeCause extends AdaptiveTestStarter {

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
      final String predecessor = dependencies.getVersions().get(version).getPredecessor();

      final MeasurementConfiguration measurementConfiguration = new MeasurementConfiguration(timeout, vms, type1error, type2error);
      final JUnitTestTransformer testtransformer = getTestTransformer(measurementConfiguration);
      testtransformer.setSumTime(measurementConfiguration.getTimeout());
      testtransformer.setDatacollectorlist(DataCollectorList.ONLYTIME);

      final CauseSearcherConfig causeSearcherConfig = new CauseSearcherConfig(version, predecessor, test);
      final BothTreeReader reader = new BothTreeReader(causeSearcherConfig, measurementConfiguration, folders);
      final boolean complete = false;
      if (complete) {
         final LevelMeasurer measurer = new LevelMeasurer(folders, causeSearcherConfig, testtransformer, measurementConfiguration);
         final CauseSearcher tester = new CauseSearcherComplete(reader, causeSearcherConfig, measurer, measurementConfiguration, folders);
         tester.search();
      } else {
         final LevelMeasurer measurer = new LevelMeasurer(folders, causeSearcherConfig, testtransformer, measurementConfiguration);
         final CauseSearcher tester = new CauseSearcher(reader, causeSearcherConfig, measurer, measurementConfiguration, folders);
         tester.search();
      }

      return null;
   }

}
