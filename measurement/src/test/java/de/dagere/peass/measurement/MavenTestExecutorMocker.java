package de.dagere.peass.measurement;

import java.io.File;

import javax.xml.bind.JAXBException;

import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import de.dagere.kopeme.datacollection.TimeDataCollector;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.Result.Fulldata.Value;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class MavenTestExecutorMocker {
   public static void mockExecutor(final MockedStatic<ExecutorCreator> creatorMock, final PeassFolders folders, final MeasurementConfig config) throws Exception {
      final TestExecutor mockedExecutor = Mockito.mock(TestExecutor.class);

      creatorMock.when(() -> ExecutorCreator.createExecutor(Mockito.any(), Mockito.any(), Mockito.any()))
      .then(new Answer<TestExecutor>() {

         @Override
         public TestExecutor answer(final InvocationOnMock invocation) throws Throwable {
            PeassFolders folders = invocation.getArgument(0);
            writeValue(folders, 100);
            return mockedExecutor;
         }
      });

      Mockito.when(mockedExecutor.getTestTransformer()).thenReturn(new JUnitTestTransformer(folders.getProjectFolder(), config));
   }

   public synchronized static void writeValue(final PeassFolders folders, final int average) throws JAXBException {
      final File measurementFile = new File(folders.getTempMeasurementFolder(), "de.peass.MyTest");
      measurementFile.mkdirs();
      final XMLDataStorer storer = new XMLDataStorer(measurementFile, "de.peass.MyTest", "test");
      Result result = buildResult(average);
      buildFulldata(average, result);

      System.out.println("Measurement file exists: " + measurementFile.exists());
      storer.storeValue(result, "de.peass.MyTest", TimeDataCollector.class.getName());
      System.out.println("Storing success");
   }

   private static void buildFulldata(final int average, final Result result) {
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
      result.setWarmup(10);
      result.setIterations(11);
      return result;
   }
}
