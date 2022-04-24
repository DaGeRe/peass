package de.dagere.peass.dependencyprocessors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;



import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.kopeme.datacollection.TestResult;
import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Fulldata;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.peass.TestUtil;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.organize.ResultOrganizer;
import de.dagere.peass.measurement.rca.helper.TestConstants;

public class TestResultOrganizer {

   public static final String VERSION_NAME = "2";

   private static final String KIEKER_TIMESTAMP = "1512123";

   private static final Logger LOG = LogManager.getLogger(TestResultOrganizer.class);

   public static final TestCase searchedTest = new TestCase("de.test.Test#testMethod");
   private PeassFolders folders;
   private ResultOrganizer organizer;
   private File methodFolder;

   @BeforeEach
   public void setUp() {
      TestUtil.deleteContents(TestConstants.CURRENT_FOLDER);
      TestUtil.deleteContents(TestConstants.CURRENT_PEASS);

      folders = new PeassFolders(TestConstants.CURRENT_FOLDER);
      methodFolder = new File(folders.getTempMeasurementFolder(), searchedTest.getClazz());
      methodFolder.mkdir();
   }

   @Test
   public void testNormalSaving() throws  IOException {
      organizer = new ResultOrganizer(folders, VERSION_NAME, 1, false, false, searchedTest, 3);

      DummyKoPeMeDataCreator.initDummyTestfile(methodFolder, 3, searchedTest);

      Assert.assertTrue(organizer.testSuccess(VERSION_NAME));
      
      organizer.saveResultFiles(VERSION_NAME, 0);

      testXMLFileExists();
   }

   @Test
   public void testKoPeMeFileSaving() throws  IOException {
      organizer = new ResultOrganizer(folders, VERSION_NAME, 1, false, false, searchedTest, 1);

      DummyKoPeMeDataCreator.initDummyTestfile(methodFolder, TestResult.BOUNDARY_SAVE_FILE * 2, searchedTest);

      organizer.saveResultFiles(VERSION_NAME, 0);

      testXMLFileIsCorrect();
   }

   private void testXMLFileIsCorrect()  {
      File kopemefile = new File(getVersionMeasurementFolder(), searchedTest.getMethod() + "_0_" + VERSION_NAME + ".xml");
      Kopemedata data = JSONDataLoader.loadData(kopemefile);
      final DatacollectorResult datacollector = data.getFirstMethodResult().getDatacollectorResults().get(0);
      final Fulldata fulldata = datacollector.getResults().get(0).getFulldata();
      Assert.assertNotNull(fulldata.getFileName());
      File fulldataFile = new File(getVersionMeasurementFolder(), fulldata.getFileName());
      Assert.assertTrue(fulldataFile.exists());
   }
   
   @Test
   public void testKiekerSavingDeletion() throws  IOException {
      organizer = new ResultOrganizer(folders, VERSION_NAME, 1, true, true, searchedTest, 1);
      organizer.getCompressor().setThresholdForZippingInMB(1);
      organizer.getCompressor().setThresholdForDeletingInMB(2);

      DummyKoPeMeDataCreator.initDummyTestfile(methodFolder, 3, searchedTest);

      writeKiekerFile(200000);

      organizer.saveResultFiles(VERSION_NAME, 0);

      testXMLFileExists();

      File versionFolder = getVersionMeasurementFolder();
      final File expectedKiekerTarFile = new File(versionFolder, KIEKER_TIMESTAMP + ".tar");
      Assert.assertFalse(expectedKiekerTarFile.exists());
      final File expectedKiekerFile = new File(versionFolder, KIEKER_TIMESTAMP);
      Assert.assertFalse(expectedKiekerFile.exists());
   }

   @Test
   public void testKiekerSavingTar() throws  IOException {
      organizer = new ResultOrganizer(folders, VERSION_NAME, 1, true, true, searchedTest, 1);
      organizer.getCompressor().setThresholdForZippingInMB(1);

      DummyKoPeMeDataCreator.initDummyTestfile(methodFolder, 3, searchedTest);

      writeKiekerFile(100000);

      organizer.saveResultFiles(VERSION_NAME, 0);

      testXMLFileExists();

      File versionFolder = getVersionMeasurementFolder();
      final File expectedKiekerTarFile = new File(versionFolder, KIEKER_TIMESTAMP + ".tar");
      Assert.assertTrue(expectedKiekerTarFile.exists());
   }

   @Test
   public void testKiekerSavingNoTar() throws  IOException {
      organizer = new ResultOrganizer(folders, VERSION_NAME, 1, true, true, searchedTest, 1);

      DummyKoPeMeDataCreator.initDummyTestfile(methodFolder, 3, searchedTest);

      writeKiekerFile(10000);

      organizer.saveResultFiles(VERSION_NAME, 0);

      testXMLFileExists();

      File versionFolder = getVersionMeasurementFolder();

      final File expectedKiekerFile = new File(versionFolder, KIEKER_TIMESTAMP);
      Assert.assertTrue(expectedKiekerFile.exists());
   }

   private File getVersionMeasurementFolder() {
      File versionFolder = new File(folders.getFullMeasurementFolder(), "measurements" + File.separator +
            searchedTest.getClazz() + File.separator +
            VERSION_NAME + File.separator +
            VERSION_NAME + File.separator);
      return versionFolder;
   }

   private void testXMLFileExists() {
      final File expectedFile = folders.getSummaryFile(searchedTest);
      Assert.assertTrue(expectedFile.exists());
   }

   private void writeKiekerFile(final int kiekerFileSize) throws IOException {
      final File kiekerFolder = new File(methodFolder, KIEKER_TIMESTAMP);
      kiekerFolder.mkdir();
      final File kiekerFile = new File(kiekerFolder, "kieker-2019.dat");
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(kiekerFile))) {

         for (int i = 0; i < kiekerFileSize; i++) {
            writer.write("1515;somekiekerstuff;" + i + "\n");
         }
         writer.flush();
         LOG.debug("Size: {} MB ({})", kiekerFile.length() / (1024 * 1024), kiekerFile.length());
      }
   }

   
}
