package de.dagere.peass.validation.temp;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
import de.dagere.peass.analysis.all.RepoFolders;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.measurement.analysis.MeasurementFileFinder;
import de.dagere.peass.utils.Constants;

public class CopyFromFull {
   private static final Logger LOG = LogManager.getLogger(CopyFromFull.class);

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException, InterruptedException, JAXBException {
      final RepoFolders folders = new RepoFolders();

      File reexecuteFolder = new File("results/reexecute-validation");
      reexecuteFolder.mkdirs();

      for (String project : GetValidationExecutionFile.VALIDATION_PROJECTS) {
         LOG.info("Analyzing {}", project);
         File executionFile = GetValidationExecutionFile.getValidationExecutionFile(project);
         final ExecutionData tests = Constants.OBJECTMAPPER.readValue(executionFile, ExecutionData.class);

         final File projectFolder = new File(folders.getCleanDataFolder(), project);
         File goal = folders.getValidationDataFolder(project);
//         mergeAllFiles(tests, projectFolder, goal);
         mergeAllFiles(tests, goal, projectFolder);
      }
   }

   private static void mergeAllFiles(final ExecutionData tests, final File source, final File goal) throws JAXBException {
      if (source.exists()) {
         for (File measurementFile : source.listFiles()) {
            if (measurementFile.getName().endsWith(".xml")) {
               checkFileMerging(goal, tests, measurementFile);
            }
         }
      }
   }

   public static void checkFileMerging(final File goal, final ExecutionData tests, final File source) throws JAXBException {
      Kopemedata kopemeData = XMLDataLoader.loadData(source);
      Testcases testcase = kopemeData.getTestcases();
      TestcaseType testcaseType = testcase.getTestcase().get(0);
      String clazz = testcase.getClazz();
      String method = testcaseType.getName();
      List<Chunk> chunks = testcaseType.getDatacollector().get(0).getChunk();
      
      if (!goal.exists()) {
         goal.mkdirs();
      }
      
      for (Chunk chunk : chunks) {
         checkChunkMerging(goal, tests, clazz, method, chunk);
      }
   }

   public static void checkChunkMerging(final File validationDataFolder, final ExecutionData tests, final String clazz, final String method, final Chunk chunk) throws JAXBException {
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

   public static boolean checkEqualVersion(final String version, final MeasurementFileFinder finder) {
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
