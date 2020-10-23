package de.peass.dependencyprocessors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.dagere.kopeme.datacollection.TestResult;
import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.organize.ResultOrganizer;

public class TestResultOrganizer {

   private static final String VERSION_NAME = "2";

   private static final String KIEKER_TIMESTAMP = "1512123";

   private static final Logger LOG = LogManager.getLogger(TestResultOrganizer.class);

   private final TestCase searchedTest = new TestCase("de.test.Test#testMethod");
   private PeASSFolders folders;
   private ResultOrganizer organizer;
   private File methodFolder;

   @Before
   public void setUp() {
      deleteOldFolders();

      folders = new PeASSFolders(new File("target/current"));
      methodFolder = new File(folders.getTempMeasurementFolder(), searchedTest.getClazz());
      methodFolder.mkdir();
   }

   private void deleteOldFolders() {
      final File resultFolder = new File("target/current_peass/");
      if (resultFolder.exists()) {
         try {
            for (final File subdir : resultFolder.listFiles()) {
               if (subdir.isDirectory()) {
                  FileUtils.deleteDirectory(subdir);
               }
            }
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }
   }

   @Test
   public void testNormalSaving() throws JAXBException, IOException {
      organizer = new ResultOrganizer(folders, VERSION_NAME, 1, false, false, searchedTest, 3);

      DummyKoPeMeDataCreator.initDummyTestfile(methodFolder, 3, searchedTest);

      Assert.assertTrue(organizer.testSuccess());
      
      organizer.saveResultFiles(VERSION_NAME, 0);

      testXMLFileExists();
      
      
   }

   @Test
   public void testKoPeMeFileSaving() throws JAXBException, IOException {
      organizer = new ResultOrganizer(folders, VERSION_NAME, 1, false, false, searchedTest, 1);

      DummyKoPeMeDataCreator.initDummyTestfile(methodFolder, TestResult.BOUNDARY_SAVE_FILE * 2, searchedTest);

      organizer.saveResultFiles(VERSION_NAME, 0);

      File kopemefile = new File(getVersionMeasurementFolder(), searchedTest.getMethod() + "_0_" + VERSION_NAME + ".xml");
      Kopemedata data = XMLDataLoader.loadData(kopemefile);
      final Datacollector datacollector = data.getTestcases().getTestcase().get(0).getDatacollector().get(0);
      final Fulldata fulldata = datacollector.getResult().get(0).getFulldata();
      Assert.assertNotNull(fulldata.getFileName());
      File fulldataFile = new File(getVersionMeasurementFolder(), fulldata.getFileName());
      Assert.assertTrue(fulldataFile.exists());
   }

   @Test
   public void testKiekerSavingTar() throws JAXBException, IOException {
      organizer = new ResultOrganizer(folders, VERSION_NAME, 1, true, true, searchedTest, 1);
      organizer.setThresholdForZippingInMB(1);

      DummyKoPeMeDataCreator.initDummyTestfile(methodFolder, 3, searchedTest);

      writeKiekerFile(100000);

      organizer.saveResultFiles(VERSION_NAME, 0);

      testXMLFileExists();

      File versionFolder = getVersionMeasurementFolder();
      final File expectedKiekerTarFile = new File(versionFolder, KIEKER_TIMESTAMP + ".tar");
      Assert.assertTrue(expectedKiekerTarFile.exists());
   }

   @Test
   public void testKiekerSavingNoTar() throws JAXBException, IOException {
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
      final File expectedFile = new File(folders.getFullMeasurementFolder(), searchedTest.getShortClazz() + "_" + searchedTest.getMethod() + ".xml");
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
