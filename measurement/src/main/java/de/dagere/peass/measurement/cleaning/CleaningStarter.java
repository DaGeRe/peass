package de.dagere.peass.measurement.cleaning;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "clean", description = "Cleans the data for faster analysis and transfer", mixinStandardHelpOptions = true)
public class CleaningStarter implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(CleaningStarter.class);

   @Option(names = { "-dependencyfile", "--dependencyfile" }, description = "Path to the dependencyfile")
   protected File dependencyFile;

   @Option(names = { "-executionfile", "--executionfile" }, description = "Path to the executionfile")
   protected File executionfile;

   @Option(names = { "-data", "--data" }, description = "Path to datafolder")
   protected File data[];

   public static void main(final String[] args) throws JAXBException, JsonParseException, JsonMappingException, IOException {
      final CommandLine commandLine = new CommandLine(new CleaningStarter());
      System.exit(commandLine.execute(args));
   }

   @Override
   public Void call() throws Exception {
      getVersionOrder();
      
      for (int i = 0; i < data.length; i++) {
         File folder = data[i];
         if (folder.getName().endsWith(PeassFolders.PEASS_POSTFIX)) {
            folder = new File(folder, "measurementsFull");
         }
         LOG.info("Searching in " + folder);
         final File cleanFolder = new File(folder.getParentFile(), "clean");
         cleanFolder.mkdirs();
         final File projectFolderName = new File(cleanFolder, folder.getName());
         if (projectFolderName.exists()) {
            throw new RuntimeException("Clean already finished - delete " + projectFolderName.getAbsolutePath() + ", if you want to clean!");
         } else {
            projectFolderName.mkdirs();
         }
         final Cleaner transformer = new Cleaner(projectFolderName);
         LOG.info("Start");
         transformer.processDataFolder(folder);
         LOG.info("Finish");
      }
      return null;
   }

   private void getVersionOrder() throws IOException, StreamReadException, DatabindException {
      Dependencies dependencies = null;
      if (dependencyFile != null) {
         dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
         VersionComparator.setDependencies(dependencies);
      }
      if (executionfile != null) {
         ExecutionData executionData = Constants.OBJECTMAPPER.readValue(executionfile, ExecutionData.class);
         dependencies = new Dependencies(executionData);
         VersionComparator.setDependencies(dependencies);
      }
      if (dependencies == null) {
         throw new RuntimeException("Dependencyfile and executionfile not readable - one needs to be defined!");
      }
   }
}
