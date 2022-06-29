package de.dagere.peass.measurement.cleaning;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.SelectedTests;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.measurement.dataloading.VersionSorter;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "clean", description = "Cleans the data for faster analysis and transfer", mixinStandardHelpOptions = true)
public class CleanStarter implements Callable<Void> {

   private static final Logger LOG = LogManager.getLogger(CleanStarter.class);

   @Option(names = { "-staticSelectionFile", "--staticSelectionFile" }, description = "Path to the staticSelectionFile")
   protected File staticSelectionFile;

   @Option(names = { "-executionfile", "--executionfile" }, description = "Path to the executionfile")
   protected File executionfile;

   @Option(names = { "-data", "--data" }, description = "Path to datafolder")
   protected File data[];

   public static void main(final String[] args) throws  JsonParseException, JsonMappingException, IOException {
      final CommandLine commandLine = new CommandLine(new CleanStarter());
      System.exit(commandLine.execute(args));
   }

   @Override
   public Void call() throws Exception {
      SelectedTests tests = VersionSorter.getSelectedTests(staticSelectionFile, executionfile);
      CommitComparatorInstance comparator = new CommitComparatorInstance(tests);
      
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
         final Cleaner transformer = new Cleaner(projectFolderName, comparator);
         LOG.info("Start");
         transformer.processDataFolder(folder);
         LOG.info("Finish");
      }
      return null;
   }
}
