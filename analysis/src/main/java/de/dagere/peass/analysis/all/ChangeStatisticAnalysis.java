package de.dagere.peass.analysis.all;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import de.dagere.peass.analysis.changes.ChangeReader;
import de.dagere.peass.config.StatisticsConfig;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.measurement.cleaning.Cleaner;
import de.dagere.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "changeStatisticAnalysis", description = "Executes the cleaning and change determination process at once for all given folders", mixinStandardHelpOptions = true)
public class ChangeStatisticAnalysis implements Callable<Void> {

   public static final String ANALYSIS_CLEANED_FOLDER = "newCleaned";
   
   private static final Logger LOG = LogManager.getLogger(ChangeStatisticAnalysis.class);

   @Option(names = { "-data", "--data" }, description = "Path to datafolder")
   private File data;

   @Option(names = { "-staticSelectionFile", "--staticSelectionFile" }, description = "Path to the staticSelectionFile")
   private File staticTestSelectionFile;

   private File cleanFolder;
   private StaticTestSelection selectedTests;
   private File projectFolderName;

   public static void main(String[] args) throws StreamReadException, DatabindException, IOException {
      final CommandLine commandLine = new CommandLine(new ChangeStatisticAnalysis());
      System.exit(commandLine.execute(args));
   }

   @Override
   public Void call() throws Exception {

      cleanFolder = new File(data, PeassFolders.CLEAN_FOLDER_NAME);
      cleanFolder.mkdirs();
      
      projectFolderName = new File(cleanFolder, ANALYSIS_CLEANED_FOLDER);
      
      selectedTests = Constants.OBJECTMAPPER.readValue(staticTestSelectionFile, StaticTestSelection.class);

      clean();
      
      getChanges();
      return null;
   }

   private void getChanges() throws FileNotFoundException {
      final ChangeReader reader = new ChangeReader(new ResultsFolders(data, "post-analysis"), null, null, selectedTests);
      reader.setConfig(new StatisticsConfig());
      reader.setTests(selectedTests.toExecutionData().getCommits());
      
      reader.readFile(projectFolderName);
   }

   private void clean() throws StreamReadException, DatabindException, IOException {
      for (File chunk : data.listFiles()) {
         if (!chunk.getName().equals(PeassFolders.CLEAN_FOLDER_NAME) && chunk.isDirectory()) {
            analyzeFolder(chunk);
         }
      }
   }

   public void analyzeFolder(File chunkFolder) throws StreamReadException, DatabindException, IOException {
      File measurementsFullFolder = new File(chunkFolder, "measurementsFull");
      
      projectFolderName.mkdirs();
      final Cleaner transformer = new Cleaner(projectFolderName, new CommitComparatorInstance(selectedTests));
      LOG.info("Start reading " + measurementsFullFolder);
      transformer.processDataFolder(measurementsFullFolder);
      LOG.info("Finish");
   }
}
