package de.dagere.peass.measurement;

import java.io.File;



import org.aspectj.util.FileUtil;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.kopeme.datacollection.TestResult;
import de.dagere.kopeme.datacollection.TimeDataCollector;
import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.TestMethod;
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
   public void testSummaryFileSaving()  {
      DummyKoPeMeDataCreator.initDummyTestfile(testFolder, TestResult.BOUNDARY_SAVE_FILE  / 2, testcase);
      
      TestMethod oneRunData = loadTestcase();
      
      final File resultFile = new File(testFolder, "result.xml");
      
      MultipleVMTestUtil.saveSummaryData(resultFile, oneResultFile, oneRunData.getDatacollectorResults().get(0).getResults().get(0), testcase, "1", 0, TimeDataCollector.class.getName());
      
      Assert.assertTrue(resultFile.exists());
   }
   
   @Test
   public void testSummaryFileSavingExternalFile()  {
      DummyKoPeMeDataCreator.initDummyTestfile(testFolder, TestResult.BOUNDARY_SAVE_FILE * 2, testcase);
      
      TestMethod oneRunData = loadTestcase();
      
      final File resultFile = new File(testFolder, "result.json");
      
      MultipleVMTestUtil.saveSummaryData(resultFile, oneResultFile, oneRunData.getDatacollectorResults().get(0).getResults().get(0), testcase, "1", 0, TimeDataCollector.class.getName());
      
      Assert.assertTrue(resultFile.exists());
   }
   
   @Test
   public void testSummaryFileSavingWithModule()  {
      TestCase testcase = new TestCase("myModule§myPackage.Test#test");
      DummyKoPeMeDataCreator.initDummyTestfile(testFolder, TestResult.BOUNDARY_SAVE_FILE * 2, testcase);
      
      TestMethod oneRunData = loadTestcase();
      
      final File resultFile = new File(testFolder, "result.json");
      
      MultipleVMTestUtil.saveSummaryData(resultFile, oneResultFile, oneRunData.getDatacollectorResults().get(0).getResults().get(0), testcase, "1", 0, TimeDataCollector.class.getName());
      
      Assert.assertTrue(resultFile.exists());
      
      Kopemedata summary = JSONDataLoader.loadData(resultFile);
      Assert.assertEquals("myModule§myPackage.Test", summary.getClazz());
      
   }

   private TestMethod loadTestcase()  {
      Kopemedata oneRunFullData = JSONDataLoader.loadData(oneResultFile);
      TestMethod oneRunData = oneRunFullData.getFirstMethodResult();
      return oneRunData;
   }
}
