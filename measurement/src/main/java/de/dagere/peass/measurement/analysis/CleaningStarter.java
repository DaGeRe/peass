package de.dagere.peass.measurement.analysis;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.measurement.analysis.Cleaner;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class CleaningStarter implements Callable<Void>{
   
   private static final Logger LOG = LogManager.getLogger(CleaningStarter.class);
   
   @Option(names = { "-dependencyfile", "--dependencyfile" }, description = "Path to the dependencyfile")
   protected File dependencyFile;

   @Option(names = { "-executionfile", "--executionfile" }, description = "Path to the executionfile")
   protected File executionFile;

   @Option(names = { "-data", "--data" }, description = "Path to datafolder")
   protected File data[];

   public static void main(final String[] args) throws JAXBException, JsonParseException, JsonMappingException, IOException {
      final CommandLine commandLine = new CommandLine(new CleaningStarter());
      System.exit(commandLine.execute(args));
   }
   
   @Override
   public Void call() throws Exception {
      VersionSorter.getVersionOrder(dependencyFile, executionFile);
      for (int i = 0; i < data.length; i++) {
         final File folder = data[i];
         LOG.info("Searching in " + folder);
         final File cleanFolder = new File(folder.getParentFile(), "clean");
         cleanFolder.mkdirs();
         final File sameNameFolder = new File(cleanFolder, folder.getName());
         sameNameFolder.mkdirs();
         final File fulldataFolder = new File(sameNameFolder, "measurementsFull");
//         fulldataFolder.mkdirs();
         final Cleaner transformer = new Cleaner(fulldataFolder);
         LOG.info("Start");
         transformer.processDataFolder(folder);
         LOG.info("Finish");
      }
      return null;
   }
}
