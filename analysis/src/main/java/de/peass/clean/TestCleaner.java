package de.peass.clean;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.persistence.Dependencies;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.measurement.analysis.Cleaner;
import de.peass.statistics.DependencyStatisticAnalyzer;
import de.peass.utils.Constants;
import de.peass.vcs.GitUtils;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "clean", description = "Cleans the data for faster analysis and transfer", mixinStandardHelpOptions = true)
public class TestCleaner implements Callable<Void> {
   
   @Option(names = { "-url", "--url" }, description = "URL for analysis - is used for determining commit order")
   private String url;
   
   @Option(names = { "-dependencyfile", "--dependencyfile" }, description = "Path to the dependencyfile")
   private File dependencyFile;
   
   @Option(names = { "-data", "--data" }, description = "Path to the data for cleaning", required = true)
   protected File[] data; 
   
   @Option(names = { "-out", "--out" }, description = "Path for saving the cleaned data")
   protected File out;

   private static final Logger LOG = LogManager.getLogger(TestCleaner.class);

   public static void main(final String[] args) throws ParseException, JAXBException, IOException {
      TestCleaner command = new TestCleaner();
      CommandLine commandLine = new CommandLine(command);
      commandLine.execute(args);
   }

   public static void cleanFolder(Cleaner transformer, final File dataFolder, final File projectNameFolder) {
      LOG.info("Start: " + dataFolder.getAbsolutePath());
      transformer.processDataFolder(dataFolder);
      LOG.info("Finish, read: " + transformer.getRead() + " correct: " + transformer.getCorrect());
   }

   public static Cleaner createCleaner(final File out, final File dataFolder, final File projectNameFolder) {
      final File cleanFolder, fulldataFolder;
      if (out == null) {
         cleanFolder = new File(projectNameFolder, "clean");
         cleanFolder.mkdirs();
         final File chunkFolder = new File(cleanFolder, dataFolder.getName());
         chunkFolder.mkdirs();
         fulldataFolder = new File(chunkFolder, "measurementsFull");
      } else {
         cleanFolder = out;
         fulldataFolder = new File(cleanFolder, projectNameFolder.getName());
      }

      final Cleaner transformer = new Cleaner(fulldataFolder);
      return transformer;
   }

   public static void getCommitOrder(final File dataFolder, final String projectName) throws JAXBException, IOException {
      if (System.getenv(Constants.PEASS_REPOS) != null) {
         final String repofolder = System.getenv(Constants.PEASS_REPOS);
         final File folder = new File(repofolder);
         if (folder.exists()) {
            final File dependencyFile = new File(folder, "dependencies-final" + File.separator + "deps_" + projectName + ".json");
            final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
            VersionComparator.setDependencies(dependencies);
         }
      }
      if (!VersionComparator.hasVersions()) {
         final String url = Constants.defaultUrls.get(projectName);
         LOG.debug("Analyzing: {} Name: {} URL: {}", dataFolder.getAbsolutePath(), projectName, url);
         if (url != null) {
            GitUtils.getCommitsForURL(url);
         }
      }
   }

   @Override
   public Void call() throws Exception {
      CleaningData cleaner = new CleaningData(out, data);

      LOG.debug("Data: {}", cleaner.getDataValue().length);

      Map<File, List<File>> commonParentFiles = sortFolders(cleaner);

      executeCleaning(cleaner, commonParentFiles);
      return null;
   }

   private void executeCleaning(CleaningData cleaner, Map<File, List<File>> commonParentFiles) throws JAXBException, IOException {
      for (Map.Entry<File, List<File>> entry : commonParentFiles.entrySet()) {
         File projectNameFolder = entry.getKey();

         final Cleaner transformer = createCleaner(cleaner.getOut(), null, projectNameFolder);

         for (File dataFolder : entry.getValue()) {
            LOG.info("Searching in " + dataFolder);

            if (dataFolder.exists()) {
               getCommitOrder(dataFolder, projectNameFolder.getName());

               if (VersionComparator.hasVersions()) {
                  cleanFolder(transformer, dataFolder, projectNameFolder);
               } else {
                  LOG.error("No URL defined.");
               }
            }
         }
      }
   }

   private Map<File, List<File>> sortFolders(CleaningData cleaner) {
      Map<File, List<File>> commonParentFiles = new HashMap<>();

      for (int i = 0; i < cleaner.getDataValue().length; i++) {
         final File dataFolder = cleaner.getDataValue()[i];
         final File projectNameFolder = dataFolder.getParentFile();
         List<File> fileList = commonParentFiles.get(projectNameFolder);
         if (fileList == null) {
            fileList = new LinkedList<>();
            commonParentFiles.put(projectNameFolder, fileList);
         }
         fileList.add(dataFolder);
      }
      return commonParentFiles;
   }

}
