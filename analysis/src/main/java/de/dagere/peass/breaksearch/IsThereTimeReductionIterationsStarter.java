package de.dagere.peass.breaksearch;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class IsThereTimeReductionIterationsStarter implements Callable<Void> {

   

   @Option(names = { "-dependencyFile", "--dependencyFile" }, description = "Internal only")
   private File dependencyFile;

   @Option(names = { "-data", "--data" }, description = "Internal only")
   private File[] data;

   public static void main(final String[] args) throws JAXBException, InterruptedException, JsonParseException, JsonMappingException, IOException {
      try {
         final CommandLine commandLine = new CommandLine(new IsThereTimeReductionIterationsStarter());
         commandLine.execute(args);
      } catch (final Throwable t) {
         t.printStackTrace();
      }
   }

  

   

   @Override
   public Void call() throws Exception {
      final StaticTestSelection dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, StaticTestSelection.class);
      VersionComparator.setDependencies(dependencies);
      
      final IsThereTimeReductionIterations isThereTimeReductionIterations = new IsThereTimeReductionIterations();
      isThereTimeReductionIterations.analyze(data);
      return null;
   }
}
