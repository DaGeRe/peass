package de.dagere.peass.dependencyprocessors;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;



import org.codehaus.plexus.util.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableWithSize;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.organize.ResultOrganizer;

public class TestResultOrganizerParams {

   private static final String VERSION = "f04b012fd14ba62aeea960ed4114eec948d16037";
   private static final String VERSION_OLD = "d77cb2ff2a446c65f0a63fd0359f9ba4dbfdb9d9";

   private static final File TEMP_MEASUREMENT_DIR = new File("target/temp_measurements");
   private static final File TEMP_DETAIL_DIR = new File(TEMP_MEASUREMENT_DIR, "measured");
   private static final File TEMP_FULL_DIR = new File(TEMP_MEASUREMENT_DIR, "measurementsFull");

   @BeforeEach
   public void init() throws IOException {
      File basicFile = new File("src/test/resources/dataConversion/measurements/");

      if (TEMP_MEASUREMENT_DIR.exists()) {
         FileUtils.cleanDirectory(TEMP_MEASUREMENT_DIR);
      } else {
         TEMP_MEASUREMENT_DIR.mkdirs();
      }

      FileUtils.copyDirectoryStructure(basicFile, TEMP_DETAIL_DIR);
      TEMP_FULL_DIR.mkdirs();
   }

   @Test
   public void testReading() throws  IOException {
      TestCase testcase = new TestCase("de.dagere.peass.ExampleBenchmarkClazz#calleeMethod");

      PeassFolders folders = mockFolders(testcase);

      for (int i = 0; i < 3; i++) {
         ResultOrganizer organizer = new ResultOrganizer(folders, VERSION, 15 + i, false, false, testcase, 10);
         organizer.saveResultFiles(VERSION, i);
         organizer.saveResultFiles(VERSION_OLD, i);
      }
      
      File expectedResultFile1 = new File(TEMP_FULL_DIR, "calleeMethod(parameter-1).xml");
      File expectedResultFile2 = new File(TEMP_FULL_DIR, "calleeMethod(parameter-2).xml");
      
      Assert.assertTrue(expectedResultFile1.exists());
      Assert.assertTrue(expectedResultFile2.exists());
      
      Kopemedata data = JSONDataLoader.loadData(expectedResultFile1);
      List<VMResult> results = data.getFirstMethodResult().getDatacollectorResults().get(0).getResults();
      MatcherAssert.assertThat(results, IsIterableWithSize.iterableWithSize(2));
      
      File expectedFulldataFile = new File(TEMP_FULL_DIR, "calleeMethod(parameter-2)_0_d77cb2ff2a446c65f0a63fd0359f9ba4dbfdb9d9.xml");
      Kopemedata fulldata = JSONDataLoader.loadData(expectedFulldataFile);
      Assert.assertEquals(1, fulldata.getFirstMethodResult().getDatacollectorResults().get(0).getResults().size());
      
      File expectedFulldataFile1 = new File(TEMP_FULL_DIR, "calleeMethod(parameter-1)_2_d77cb2ff2a446c65f0a63fd0359f9ba4dbfdb9d9.xml");
      Kopemedata fulldata1 = JSONDataLoader.loadData(expectedFulldataFile1);
      Assert.assertEquals(1, fulldata1.getFirstMethodResult().getDatacollectorResults().get(0).getResults().size());
   }

   private PeassFolders mockFolders(final TestCase testcase) {
      PeassFolders folders = Mockito.mock(PeassFolders.class);
      Mockito.when(folders.getDetailResultFolder()).thenReturn(TEMP_DETAIL_DIR);
      Mockito.when(folders.getFullMeasurementFolder()).thenReturn(TEMP_FULL_DIR);
      Mockito.when(folders.getSummaryFile(testcase)).thenAnswer(new Answer<File>() {

         @Override
         public File answer(final InvocationOnMock invocation) throws Throwable {
            TestCase test = invocation.getArgument(0);
            return new File(TEMP_FULL_DIR, test.getMethod() + "(" + test.getParams() + ").xml");
         }
      });

      Mockito.when(folders.getResultFile(Mockito.any(), Mockito.anyInt(), Mockito.any(), Mockito.any())).thenAnswer(new Answer<File>() {

         @Override
         public File answer(final InvocationOnMock invocation) throws Throwable {
            TestCase test = invocation.getArgument(0);
            int index = invocation.getArgument(1);
            String version = invocation.getArgument(2);
            File resultFile = new File(TEMP_FULL_DIR, test.getMethod() + "(" + test.getParams() + ")_" + index + "_" + version + ".xml");
            return resultFile;
         }
      });
      Mockito.when(folders.findTempClazzFolder(testcase)).thenAnswer(new Answer<List<File>>() {
         int index = 0;

         @Override
         public List<File> answer(final InvocationOnMock invocation) throws Throwable {
            File resultFile = new File(TEMP_DETAIL_DIR, "measurements" + index);
            index++;
            return Collections.singletonList(resultFile);
         }
      });
      return folders;
   }
}
