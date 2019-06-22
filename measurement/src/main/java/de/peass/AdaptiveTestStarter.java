package de.peass;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import de.peass.dependencyprocessors.AdaptiveTester;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.utils.OptionConstants;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Executes test and skips those where results clearly indicate a performance change
 * 
 * @author reichelt
 *
 */
@Command(description = "Measures the defined tests and versions until agnostic t-test makes a non-agnostic decission", name = "measure")
public final class AdaptiveTestStarter extends DependencyTestPairStarter {

   public AdaptiveTestStarter() throws JAXBException, IOException {

   }

   public static void main(final String[] args) throws JAXBException, IOException {
      AdaptiveTestStarter command = new AdaptiveTestStarter();
      CommandLine commandLine = new CommandLine(command);
      commandLine.execute(args);
      
   }

   @Override
   public Void call() throws Exception {
      super.call();
      final JUnitTestTransformer testgenerator = getTestTransformer();
      tester = new AdaptiveTester(folders, testgenerator, vms);
      tester.setTimeout(timeout);
      
      processCommandline();
      return null;
   }

}
