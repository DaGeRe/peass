package de.dagere.peass.measurement;

import java.io.File;

import javax.xml.bind.JAXBException;

import org.aspectj.util.FileUtil;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.kopeme.datacollection.TestResult;
import de.dagere.kopeme.datacollection.TimeDataCollector;
import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependencyprocessors.DummyKoPeMeDataCreator;
import de.dagere.peass.measurement.dataloading.MultipleVMTestUtil;

public class TestSummaryFileSaving {
   
   private static final File testFolder = new File("target/current_peass");
   private static final TestCase testcase = new TestCase("Test#test");
   private static final File oneResultFile = new File(testFolder, "test.xml");
   
   @BeforeEach
   public void cleanup() {
      FileUtil.deleteContents(testFolder);
      if (!testFolder.exists()) {
         testFolder.mkdirs();
      }
   }
   
   @Test
   public void testSummaryFileSaving() throws JAXBException {
      DummyKoPeMeDataCreator.initDummyTestfile(testFolder, TestResult.BOUNDARY_SAVE_FILE  / 2, testcase);
      
      TestcaseType oneRunData = loadTestcase();
      
      final File resultFile = new File(testFolder, "result.xml");
      
      MultipleVMTestUtil.saveSummaryData(resultFile, oneResultFile, oneRunData.getDatacollector().get(0).getResult().get(0), testcase, "1", 0, TimeDataCollector.class.getName());
      
      Assert.assertTrue(resultFile.exists());
   }
   
   @Test
   public void testSummaryFileSavingExternalFile() throws JAXBException {
      DummyKoPeMeDataCreator.initDummyTestfile(testFolder, TestResult.BOUNDARY_SAVE_FILE * 2, testcase);
      
      TestcaseType oneRunData = loadTestcase();
      
      final File resultFile = new File(testFolder, "result.xml");
      
      MultipleVMTestUtil.saveSummaryData(resultFile, oneResultFile, oneRunData.getDatacollector().get(0).getResult().get(0), testcase, "1", 0, TimeDataCollector.class.getName());
      
      Assert.assertTrue(resultFile.exists());
   }
   
   @Test
   public void testSummaryFileSavingWithModule() throws JAXBException {
      TestCase testcase = new TestCase("myModule§myPackage.Test#test");
      DummyKoPeMeDataCreator.initDummyTestfile(testFolder, TestResult.BOUNDARY_SAVE_FILE * 2, testcase);
      
      TestcaseType oneRunData = loadTestcase();
      
      final File resultFile = new File(testFolder, "result.xml");
      
      MultipleVMTestUtil.saveSummaryData(resultFile, oneResultFile, oneRunData.getDatacollector().get(0).getResult().get(0), testcase, "1", 0, TimeDataCollector.class.getName());
      
      Assert.assertTrue(resultFile.exists());
      
      Kopemedata summary = XMLDataLoader.loadData(resultFile);
      Assert.assertEquals("myModule§myPackage.Test", summary.getTestcases().getClazz());
      
   }

   private TestcaseType loadTestcase() throws JAXBException {
      final XMLDataLoader loader = new XMLDataLoader(oneResultFile);
      loader.readFulldataValues();
      Kopemedata oneRunFullData = loader.getFullData();
      TestcaseType oneRunData = oneRunFullData.getTestcases().getTestcase().get(0);
      return oneRunData;
   }
}
