package de.peass.measurement;

import java.io.File;

import javax.xml.bind.JAXBException;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

import de.dagere.kopeme.datacollection.TimeDataCollector;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.Result.Fulldata.Value;
import de.peass.dependency.ExecutorCreator;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.execution.MavenTestExecutor;
import de.peass.dependency.execution.TestExecutor;
import de.peass.testtransformation.JUnitTestTransformer;

public class MavenTestExecutorMocker {
   public static void mockExecutor() {
      final MavenTestExecutor manager = Mockito.mock(MavenTestExecutor.class);

      PowerMockito.mockStatic(ExecutorCreator.class);
      PowerMockito.doAnswer(new Answer<MavenTestExecutor>() {

         @Override
         public MavenTestExecutor answer(final InvocationOnMock invocation) throws Throwable {
            return manager;
         }
      }).when(ExecutorCreator.class);
      ExecutorCreator.createExecutor(Mockito.any(PeASSFolders.class), Mockito.any(JUnitTestTransformer.class));
   }
   
   public static void mockExecutor(final PeASSFolders folders) {
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
   }
   
   public static void writeValue(final PeASSFolders folders, final int average) throws JAXBException {
      final File measurementFile = new File(folders.getTempMeasurementFolder(), "de.peass.MyTest");
      measurementFile.mkdirs();
      final XMLDataStorer storer = new XMLDataStorer(measurementFile, "de.peass.MyTest", "test");
      Result result = buildResult(average);
      buildFulldata(average, result);

      storer.storeValue(result, "de.peass.MyTest", TimeDataCollector.class.getName());
   }
   
   private static void buildFulldata(final int average, Result result) {
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

   private static Result buildResult(final int average) {
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
