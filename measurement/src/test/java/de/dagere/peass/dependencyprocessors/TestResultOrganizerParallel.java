package de.dagere.peass.dependencyprocessors;

import java.io.File;
import java.io.IOException;



import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.kopeme.datacollection.TestResult;
import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Fulldata;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.VMResultChunk;
import de.dagere.peass.TestUtil;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.organize.ResultOrganizerParallel;
import de.dagere.peass.measurement.rca.helper.TestConstants;

public class TestResultOrganizerParallel {

   private final static String PARALLEL_VERSION = "1";
   
   private PeassFolders folders;
   private ResultOrganizerParallel organizer;
   private File methodFolder;

   @BeforeEach
   public void setUp() {
      TestUtil.deleteContents(TestConstants.CURRENT_FOLDER);
      TestUtil.deleteContents(TestConstants.CURRENT_PEASS);

      folders = new PeassFolders(new File("target/current"));

   }

   @Test
   public void testParallelSaving() throws  IOException {
      organizer = new ResultOrganizerParallel(folders, TestResultOrganizer.VERSION_NAME, 1, false, false, TestResultOrganizer.searchedTest, 3);

      PeassFolders parallelProjectFolders = initFolders();

      DummyKoPeMeDataCreator.initDummyTestfile(methodFolder, 3, TestResultOrganizer.searchedTest);

      organizer.addVersionFolders(PARALLEL_VERSION, parallelProjectFolders);

      Assert.assertTrue(organizer.testSuccess("1"));
   }

   @Test
   public void testKoPeMeFileSaving() throws  IOException {
      organizer = new ResultOrganizerParallel(folders, TestResultOrganizer.VERSION_NAME, 1, false, false, TestResultOrganizer.searchedTest, TestResult.BOUNDARY_SAVE_FILE * 2);

      PeassFolders parallelProjectFolders = initFolders();

      DummyKoPeMeDataCreator.initDummyTestfile(methodFolder, TestResult.BOUNDARY_SAVE_FILE * 2, TestResultOrganizer.searchedTest);

      organizer.addVersionFolders(PARALLEL_VERSION, parallelProjectFolders);

      Assert.assertTrue(organizer.testSuccess("1"));
      
      organizer.saveResultFiles(PARALLEL_VERSION, 0);
      testXMLFileIsCorrect();
   }

   private void testXMLFileIsCorrect()  {
      File kopemefile = new File(getVersionMeasurementFolder(TestResultOrganizer.VERSION_NAME, PARALLEL_VERSION), TestResultOrganizer.searchedTest.getMethod() + "_0_" + PARALLEL_VERSION + ".xml");
      final Kopemedata data = JSONDataLoader.loadData(kopemefile);
      final DatacollectorResult collector = data.getFirstMethodResult().getDatacollectorResults().get(0);
      final VMResultChunk chunk = collector.getChunks().get(0);
      final Fulldata fulldata = chunk.getResults().get(0).getFulldata();
      Assert.assertNotNull(fulldata.getFileName());
      File fulldataFile = new File(getVersionMeasurementFolder(TestResultOrganizer.VERSION_NAME, PARALLEL_VERSION), fulldata.getFileName());
      Assert.assertTrue(fulldataFile.exists());
   }
   
   private File getVersionMeasurementFolder(final String mainVersion, final String subVersion) {
      File versionFolder = new File(folders.getFullMeasurementFolder(), "measurements" + File.separator +
            TestResultOrganizer.searchedTest.getClazz() + File.separator +
            mainVersion + File.separator +
            subVersion + File.separator);
      return versionFolder;
   }
   
   private PeassFolders initFolders() {
      File parallelProjectFolder = new File(folders.getTempProjectFolder(), PARALLEL_VERSION);
      PeassFolders parallelProjectFolders = new PeassFolders(parallelProjectFolder);
      methodFolder = new File(parallelProjectFolders.getTempMeasurementFolder(), TestResultOrganizer.searchedTest.getClazz());
      methodFolder.mkdir();
      return parallelProjectFolders;
   }
}
