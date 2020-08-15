package de.peass.measurement.analysis;

import java.io.File;
import java.io.IOException;

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
import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.Result.Fulldata.Value;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.dependency.ExecutorCreator;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependency.execution.TestExecutor;
import de.peass.dependencyprocessors.DependencyTester;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.vcs.VersionControlSystem;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ VersionControlSystem.class, ExecutorCreator.class })
@PowerMockIgnore("javax.management.*")
public class TestDependencyTester {

   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   @Test
   public void testFiles() throws IOException, InterruptedException, JAXBException {
      final PeASSFolders folders = new PeASSFolders(folder.getRoot());
      final MeasurementConfiguration configuration = new MeasurementConfiguration(4, "2", "1");

      final JUnitTestTransformer testTransformer = Mockito.mock(JUnitTestTransformer.class);
      Mockito.when(testTransformer.getConfig()).thenReturn(configuration);

      final TestExecutor mockedExecutor = Mockito.mock(TestExecutor.class);
      Mockito.doAnswer(new Answer<Void>() {

         @Override
         public Void answer(final InvocationOnMock invocation) throws Throwable {
            writeValue(folders, 100);
            return null;
         }

      }).when(mockedExecutor).executeTest(Mockito.any(), Mockito.any(), Mockito.anyLong());

      PowerMockito.mockStatic(ExecutorCreator.class);
      PowerMockito.when(ExecutorCreator.createExecutor(Mockito.any(), Mockito.any()))
            .thenReturn(mockedExecutor);

      PowerMockito.mockStatic(VersionControlSystem.class);
      PowerMockito.when(VersionControlSystem.getVersionControlSystem(folder.getRoot()))
            .thenReturn(VersionControlSystem.GIT);

      final DependencyTester tester = new DependencyTester(folders, testTransformer);

      tester.evaluate(new TestCase("de.peass.MyTest", "test"));

      final File expectedShortresultFile = new File(folders.getFullMeasurementFolder(), "MyTest_test.xml");
      Assert.assertTrue(expectedShortresultFile.exists());

      final Kopemedata data = XMLDataLoader.loadData(expectedShortresultFile);
      final Datacollector collector = data.getTestcases().getTestcase().get(0).getDatacollector().get(0);
      final Chunk chunk = collector.getChunk().get(0);
      Assert.assertEquals(105, chunk.getResult().get(0).getValue(), 0.1);
      Assert.assertEquals(5, chunk.getResult().get(0).getRepetitions());
      Assert.assertEquals(11, chunk.getResult().get(0).getExecutionTimes());
      Assert.assertEquals(10, chunk.getResult().get(0).getWarmupExecutions());
   }

   public void writeValue(final PeASSFolders folders, final int average) throws JAXBException {
      final File measurementFile = new File(folders.getTempMeasurementFolder(), "de.peass.MyTest");
      measurementFile.mkdirs();
      final XMLDataStorer storer = new XMLDataStorer(measurementFile, "de.peass.MyTest", "test");
      Result result = buildResult(average);
      buildFulldata(average, result);

      storer.storeValue(result, "de.peass.MyTest", TimeDataCollector.class.getName());
   }

   private void buildFulldata(final int average, Result result) {
      final Fulldata values = new Fulldata();
      for (long i = average - 10; i <= average + 10; i++) {
         Value value = new Value();
         value.setStart(i);
         value.setValue(i);
         values.getValue().add(value);
         // values.put(i, i);
      }
      result.setFulldata(values);
   }

   private Result buildResult(final int average) {
      Result result = new Result();
      result.setValue(average);
      result.setDeviation(1);
      result.setMin(0D);
      result.setMax(20D);
      result.setRepetitions(5);
      result.setWarmupExecutions(10);
      result.setExecutionTimes(11);
      return result;
   }
}
