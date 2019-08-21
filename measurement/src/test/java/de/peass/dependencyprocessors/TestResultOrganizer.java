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

import de.dagere.kopeme.datacollection.TimeDataCollector;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.Result.Fulldata.Value;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.analysis.MultipleVMTestUtil;
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
      organizer = new ResultOrganizer(folders, VERSION_NAME, 1, false);
      
      initDummyTestfile(methodFolder);

      organizer.saveResultFiles(searchedTest, VERSION_NAME, 0);

      testXMLFileExists();
   }

   @Test
   public void testKiekerSavingTar() throws JAXBException, IOException {
      organizer = new ResultOrganizer(folders, VERSION_NAME, 1, true);
      organizer.setThresholdForZippingInMB(1);
      
      initDummyTestfile(methodFolder);
      
      writeKiekerFile(100000);
      
      organizer.saveResultFiles(searchedTest, VERSION_NAME, 0);
      
      testXMLFileExists();
      
      final File expectedKiekerTarFile = new File(folders.getFullMeasurementFolder(), "measurements" + File.separator +  
            searchedTest.getClazz() + File.separator +
            VERSION_NAME + File.separator + 
            VERSION_NAME + File.separator + 
            KIEKER_TIMESTAMP + ".tar");
      Assert.assertTrue(expectedKiekerTarFile.exists());
   }
   
   @Test
   public void testKiekerSavingNoTar() throws JAXBException, IOException {
      organizer = new ResultOrganizer(folders, VERSION_NAME, 1, true);
      
      initDummyTestfile(methodFolder);
      
      writeKiekerFile(10000);
      
      organizer.saveResultFiles(searchedTest, VERSION_NAME, 0);
      
      testXMLFileExists();
      
      final File expectedKiekerFile = new File(folders.getFullMeasurementFolder(), "measurements" + File.separator +  
            searchedTest.getClazz() + File.separator +
            VERSION_NAME + File.separator + 
            VERSION_NAME + File.separator + 
            KIEKER_TIMESTAMP);
      Assert.assertTrue(expectedKiekerFile.exists());
   }

   private void testXMLFileExists() {
      final File expectedFile = new File(folders.getFullMeasurementFolder(), searchedTest.getShortClazz() + "_" + searchedTest.getMethod() + ".xml");
      Assert.assertTrue(expectedFile.exists());
   }

   private void writeKiekerFile(final int kiekerFileSize) throws IOException {
      final File kiekerFolder = new File(methodFolder, KIEKER_TIMESTAMP);
      kiekerFolder.mkdir();
      final File kiekerFile = new File(kiekerFolder, "kieker-2019.dat");
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(kiekerFile))){
         
         for (int i = 0; i < kiekerFileSize; i++) {
            writer.write("1515;somekiekerstuff;" + i + "\n");
         }
         writer.flush();
         LOG.debug("Size: {} MB ({})", kiekerFile.length() / (1024*1024), kiekerFile.length());
      }
   }

   private void initDummyTestfile(final File methodFolder) throws JAXBException {
      final File kopemeFile = new File(methodFolder, "testMethod.xml");

      final Kopemedata currentdata = MultipleVMTestUtil.initKopemeData(kopemeFile, searchedTest);
      final Datacollector collector = new Datacollector();
      collector.setName(TimeDataCollector.class.getName());
      final TestcaseType testcaseType = currentdata.getTestcases().getTestcase().get(0);
      testcaseType.getDatacollector().add(collector);
      final Result result = new Result();
      result.setValue(15);
      collector.getResult().add(result);
      initDummyFulldata(result);

      XMLDataStorer.storeData(kopemeFile, currentdata);
   }

   private void initDummyFulldata(final Result result) {
      result.setFulldata(new Fulldata());
      final Value value = new Value();
      value.setValue("15");
      result.getFulldata().getValue().add(value);
      result.getFulldata().getValue().add(value);
      result.getFulldata().getValue().add(value);
   }
}
