package de.peass;

import java.io.IOException;

import javax.annotation.PreDestroy;
import javax.xml.bind.JAXBException;

import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependencyprocessors.AdaptiveTester;
import de.peass.dependencyprocessors.VersionProcessor;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.searchcause.CauseSearcher;
import de.peass.testtransformation.JUnitTestTransformer;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(description = "Searches for root cause of a performance change, i.e. method causing the performance change", name = "searchcause")
public class SearchChangeCause extends AdaptiveTestStarter {

   public static void main(final String[] args) throws JAXBException, IOException {
      SearchChangeCause command = new SearchChangeCause();
      CommandLine commandLine = new CommandLine(command);
      commandLine.execute(args);

   }

   public SearchChangeCause() throws JAXBException, IOException {
      super();
   }

   @Override
   public Void call() throws Exception {
      initVersionProcessor();
      

      MeasurementConfiguration config = new MeasurementConfiguration(timeout, vms, type1error, type2error);
      final JUnitTestTransformer testgenerator = getTestTransformer();
      testgenerator.setSumTime(config.getTimeout());
      TestCase test = new TestCase(testName);
      String predecessor = dependencies.getVersions().get(version).getPredecessor();
      CauseSearcher tester = new CauseSearcher(folders.getProjectFolder(), version, predecessor, test, testgenerator, config);
      tester.search();
      return null;
   }

}
