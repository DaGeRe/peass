package de.peass.measurement.analysis;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.dagere.kopeme.datacollection.TimeDataCollector;
import de.dagere.kopeme.datastorage.PerformanceDataMeasure;
import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.TestResultManager;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.TestExecutor;
import de.peass.dependencyprocessors.DependencyTester;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.vcs.VersionControlSystem;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ VersionControlSystem.class, TestResultManager.class })
@PowerMockIgnore("javax.management.*")
public class TestDependencyTester {

   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   @Test
   public void testFiles() throws IOException, InterruptedException, JAXBException {
      PeASSFolders folders = new PeASSFolders(folder.getRoot());
      JUnitTestTransformer testTransformer = Mockito.mock(JUnitTestTransformer.class);

      TestExecutor mockedExecutor = Mockito.mock(TestExecutor.class);
      Mockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(InvocationOnMock invocation) throws Throwable {
            writeValue(folders, 100);
            return null;
         }

      }).when(mockedExecutor).executeTest(Mockito.any(), Mockito.any(), Mockito.anyLong());

      PowerMockito.mockStatic(TestResultManager.class);
      PowerMockito.when(TestResultManager.createExecutor(Mockito.any(), Mockito.anyLong(), Mockito.any()))
            .thenReturn(mockedExecutor);

      PowerMockito.mockStatic(VersionControlSystem.class);
      PowerMockito.when(VersionControlSystem.getVersionControlSystem(folder.getRoot()))
            .thenReturn(VersionControlSystem.GIT);

      DependencyTester tester = new DependencyTester(folders, testTransformer, 4);

      tester.evaluate("1", "2", new TestCase("de.peass.MyTest", "test"));
      
      File expectedShortresultFile = new File(folders.getFullMeasurementFolder(), "MyTest_test.xml");
      Assert.assertTrue(expectedShortresultFile.exists());
      
      Kopemedata data = XMLDataLoader.loadData(expectedShortresultFile);
      Datacollector collector = data.getTestcases().getTestcase().get(0).getDatacollector().get(0);
      Chunk chunk = collector.getChunk().get(0);
      Assert.assertEquals(105, chunk.getResult().get(0).getValue(), 0.1);
      Assert.assertEquals(5, chunk.getResult().get(0).getRepetitions());
      Assert.assertEquals(11, chunk.getResult().get(0).getExecutionTimes());
      Assert.assertEquals(10, chunk.getResult().get(0).getWarmupExecutions());
   }
   
   public void writeValue(PeASSFolders folders, int average) throws JAXBException {
      File measurementFile = new File(folders.getTempMeasurementFolder(), "de.peass.MyTest");
      measurementFile.mkdirs();
      XMLDataStorer storer = new XMLDataStorer(measurementFile, "de.peass.MyTest", "test");
      PerformanceDataMeasure measure = new PerformanceDataMeasure("de.peass.MyTest.test", TimeDataCollector.class.getName(), average, 1,
            20, 0, 5, average - 10, 11, 5);
      Map<Long, Long> values = new LinkedHashMap<>();
      for (long i = average - 10; i <= average + 10; i++) {
         values.put(i, i);
      }

      storer.storeValue(measure, values);
      storer.storeData();
   }
}
