package de.peass.validation.temp;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Kopemedata.Testcases;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.analysis.all.RepoFolders;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.measurement.analysis.MeasurementFileFinder;
import de.peran.FolderSearcher;

public class CopyFromFull {
   private static final Logger LOG = LogManager.getLogger(CopyFromFull.class);

   public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException, InterruptedException, JAXBException {
      final RepoFolders folders = new RepoFolders();

      File reexecuteFolder = new File("results/reexecute-validation");
      reexecuteFolder.mkdirs();

      for (String project : GetValidationExecutionFile.VALIDATION_PROJECTS) {
         LOG.info("Analyzing {}", project);
         File executionFile = GetValidationExecutionFile.getValidationExecutionFile(project);
         final ExecutionData tests = FolderSearcher.MAPPER.readValue(executionFile, ExecutionData.class);

         final File projectFolder = new File(folders.getCleanDataFolder(), project);
         if (projectFolder.exists()) {
            for (File measurementFile : projectFolder.listFiles()) {
               if (measurementFile.getName().endsWith(".xml")) {
                  checkFileMerging(folders, project, tests, measurementFile);
               }
            }
         }
      }
   }

   public static void checkFileMerging(final RepoFolders folders, String project, final ExecutionData tests, File measurementFile) throws JAXBException {
      Kopemedata kopemeData = XMLDataLoader.loadData(measurementFile);
      Testcases testcase = kopemeData.getTestcases();
      TestcaseType testcaseType = testcase.getTestcase().get(0);
      String clazz = testcase.getClazz();
      String method = testcaseType.getName();
      List<Chunk> chunks = testcaseType.getDatacollector().get(0).getChunk();
      
      File validationDataFolder = folders.getValidationDataFolder(project);
      if (!validationDataFolder.exists()) {
         validationDataFolder.mkdirs();
      }
      
      for (Chunk chunk : chunks) {
         checkChunkMerging(validationDataFolder, project, tests, clazz, method, chunk);
      }
   }

   public static void checkChunkMerging(final File validationDataFolder, String project, final ExecutionData tests, String clazz, String method, Chunk chunk) throws JAXBException {
      String version = chunk.getResult().get(chunk.getResult().size() - 1).getVersion().getGitversion();
      if (tests.getVersions().containsKey(version)) {
         System.out.println(version);

         
         MeasurementFileFinder finder = new MeasurementFileFinder(validationDataFolder, clazz, method);
         boolean otherHasAlreadyData = checkEqualVersion(version, finder);
         if (!otherHasAlreadyData) {
            finder.getDataCollector().getChunk().add(chunk);
            XMLDataStorer.storeData(finder.getMeasurementFile(), finder.getOneResultData());
         }
      }
   }

   public static boolean checkEqualVersion(String version, MeasurementFileFinder finder) {
      boolean otherHasAlreadyData = false;
      for (Chunk otherChunk : finder.getDataCollector().getChunk()) {
         String otherVersion = otherChunk.getResult().get(otherChunk.getResult().size() - 1).getVersion().getGitversion();
         if (otherVersion.equals(version)) {
            otherHasAlreadyData = true;
         }
      }
      return otherHasAlreadyData;
   }
}
