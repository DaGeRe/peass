package de.peass.measurement;

import java.io.File;

import javax.xml.bind.JAXBException;

import org.aspectj.util.FileUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.dagere.kopeme.datacollection.TestResult;
import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.TestcaseType;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependencyprocessors.DummyKoPeMeDataCreator;
import de.peass.measurement.analysis.MultipleVMTestUtil;

public class TestSummaryFileSaving {
   
   private static final File testFolder = new File("target/current_peass");
   private static final TestCase testcase = new TestCase("Test#test");
   private static final File oneResultFile = new File(testFolder, "test.xml");
   
   @Before
   public void cleanup() {
      FileUtil.deleteContents(testFolder);
      if (!testFolder.exists()) {
         testFolder.mkdirs();
      }
   }
   
   @Test
   public void testSummaryFileSaving() throws JAXBException {
      DummyKoPeMeDataCreator.initDummyTestfile(testFolder, TestResult.BOUNDARY_SAVE_FILE  / 2, testcase);
      
      final XMLDataLoader loader = new XMLDataLoader(oneResultFile);
      loader.readFulldataValues();
      Kopemedata oneRunFullData = loader.getFullData();
      TestcaseType oneRunData = oneRunFullData.getTestcases().getTestcase().get(0);
      
      final File resultFile = new File(testFolder, "result.xml");
      
      MultipleVMTestUtil.saveSummaryData(resultFile, oneResultFile, oneRunData, testcase, "1", 0);
      
      Assert.assertTrue(resultFile.exists());
   }
   
   @Test
   public void testSummaryFileSavingExternalFile() throws JAXBException {
      DummyKoPeMeDataCreator.initDummyTestfile(testFolder, TestResult.BOUNDARY_SAVE_FILE * 2, testcase);
      
      final XMLDataLoader loader = new XMLDataLoader(oneResultFile);
      loader.readFulldataValues();
      Kopemedata oneRunFullData = loader.getFullData();
      TestcaseType oneRunData = oneRunFullData.getTestcases().getTestcase().get(0);
      
      final File resultFile = new File(testFolder, "result.xml");
      
      MultipleVMTestUtil.saveSummaryData(resultFile, oneResultFile, oneRunData, testcase, "1", 0);
      
      Assert.assertTrue(resultFile.exists());
   }
}
