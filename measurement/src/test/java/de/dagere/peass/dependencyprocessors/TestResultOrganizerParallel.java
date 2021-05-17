package de.dagere.peass.dependencyprocessors;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.kopeme.datacollection.TestResult;
import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.peass.TestUtil;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.measurement.organize.ResultOrganizerParallel;

public class TestResultOrganizerParallel {

   private final static String PARALLEL_VERSION = "1";
   
   private PeASSFolders folders;
   private ResultOrganizerParallel organizer;
   private File methodFolder;

   @BeforeEach
   public void setUp() {
      TestUtil.deleteOldFolders();

      folders = new PeASSFolders(new File("target/current"));

   }

   @Test
   public void testParallelSaving() throws JAXBException, IOException {
      organizer = new ResultOrganizerParallel(folders, TestResultOrganizer.VERSION_NAME, 1, false, false, TestResultOrganizer.searchedTest, 3);

      PeASSFolders parallelProjectFolders = initFolders();

      DummyKoPeMeDataCreator.initDummyTestfile(methodFolder, 3, TestResultOrganizer.searchedTest);

      organizer.addVersionFolders(PARALLEL_VERSION, parallelProjectFolders);

      Assert.assertTrue(organizer.testSuccess("1"));
   }

   @Test
   public void testKoPeMeFileSaving() throws JAXBException, IOException {
      organizer = new ResultOrganizerParallel(folders, TestResultOrganizer.VERSION_NAME, 1, false, false, TestResultOrganizer.searchedTest, TestResult.BOUNDARY_SAVE_FILE * 2);

      PeASSFolders parallelProjectFolders = initFolders();

      DummyKoPeMeDataCreator.initDummyTestfile(methodFolder, TestResult.BOUNDARY_SAVE_FILE * 2, TestResultOrganizer.searchedTest);

      organizer.addVersionFolders(PARALLEL_VERSION, parallelProjectFolders);

      Assert.assertTrue(organizer.testSuccess("1"));
      
      organizer.saveResultFiles(PARALLEL_VERSION, 0);
      testXMLFileIsCorrect();
   }

   private void testXMLFileIsCorrect() throws JAXBException {
      File kopemefile = new File(getVersionMeasurementFolder(TestResultOrganizer.VERSION_NAME, PARALLEL_VERSION), TestResultOrganizer.searchedTest.getMethod() + "_0_" + PARALLEL_VERSION + ".xml");
      Kopemedata data = XMLDataLoader.loadData(kopemefile);
      final Datacollector datacollector = data.getTestcases().getTestcase().get(0).getDatacollector().get(0);
      final Fulldata fulldata = datacollector.getResult().get(0).getFulldata();
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
   
   private PeASSFolders initFolders() {
      File parallelProjectFolder = new File(folders.getTempProjectFolder(), PARALLEL_VERSION);
      PeASSFolders parallelProjectFolders = new PeASSFolders(parallelProjectFolder);
      methodFolder = new File(parallelProjectFolders.getTempMeasurementFolder(), TestResultOrganizer.searchedTest.getClazz());
      methodFolder.mkdir();
      return parallelProjectFolders;
   }
}
