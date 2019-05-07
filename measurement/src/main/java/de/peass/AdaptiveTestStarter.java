package de.peass;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;

import de.peass.dependencyprocessors.AdaptiveTester;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.utils.OptionConstants;

/**
 * Executes test and skips those where results clearly indicate a performance change
 * 
 * @author reichelt
 *
 */
public class AdaptiveTestStarter extends DependencyTestPairStarter {

   public AdaptiveTestStarter(final String[] args) throws ParseException, JAXBException, IOException {
      super(args);
      //TODO Code duplication is not good style..
      final int vms = Integer.parseInt(line.getOptionValue(OptionConstants.VMS.getName(), "100"));
      final long timeout = Integer.parseInt(line.getOptionValue(OptionConstants.TIMEOUT.getName(), "120")); // Default: 2 Hours
      final JUnitTestTransformer testgenerator = DependencyTestPairStarter.getTestTransformer(line, folders);
      tester = new AdaptiveTester(folders, true, testgenerator, vms);
      tester.setTimeout(timeout);
   }

   public static void main(final String[] args) throws ParseException, JAXBException, IOException {
      final AdaptiveTestStarter starter = new AdaptiveTestStarter(args);
      starter.processCommandline();
   }

}
