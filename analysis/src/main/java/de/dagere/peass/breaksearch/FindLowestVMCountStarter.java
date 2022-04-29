package de.dagere.peass.breaksearch;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.utils.Constants;

import picocli.CommandLine;
import picocli.CommandLine.Option;

public class FindLowestVMCountStarter  implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(FindLowestVMCountStarter.class);

   @Option(names = { "-dependencyFile", "--dependencyFile" }, description = "Internal only")
   private File dependencyFile;
   
   @Option(names = { "-data", "--data" }, description = "Internal only")
   private File[] data;
   
   public static void main(final String[] args) throws  InterruptedException, JsonParseException, JsonMappingException, IOException {
      try {
         final CommandLine commandLine = new CommandLine(new FindLowestVMCountStarter());
         commandLine.execute(args);
      } catch (final Throwable t) {
         t.printStackTrace();
      }
   }

   @Override
   public Void call() throws Exception {
      final StaticTestSelection dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, StaticTestSelection.class);
      VersionComparator.setDependencies(dependencies);
      
      final FindLowestVMCounter flv = new FindLowestVMCounter();
      for (File folder : data) {
         LOG.info("Searching in " + folder);
         flv.processDataFolder(folder);
      }
      return null;
   }
   
   
}
