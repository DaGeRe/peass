package de.dagere.peass.measurement;

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
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.execution.MavenTestExecutor;
import de.dagere.peass.dependency.execution.TestExecutor;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

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
      ExecutorCreator.createExecutor(Mockito.any(PeASSFolders.class), Mockito.any(JUnitTestTransformer.class), Mockito.any(EnvironmentVariables.class));
   }

   public static void mockExecutor(final PeASSFolders folders, final MeasurementConfiguration config) throws Exception {
      final TestExecutor mockedExecutor = Mockito.mock(TestExecutor.class);

      PowerMockito.mockStatic(ExecutorCreator.class);
      PowerMockito.when(ExecutorCreator.createExecutor(Mockito.any(), Mockito.any(), Mockito.any()))
            .then(new Answer<TestExecutor>() {

               @Override
               public TestExecutor answer(final InvocationOnMock invocation) throws Throwable {
                  PeASSFolders folders = invocation.getArgument(0);
                  writeValue(folders, 100);
                  return mockedExecutor;
               }
            });

      Mockito.when(mockedExecutor.getTestTransformer()).thenReturn(new JUnitTestTransformer(folders.getProjectFolder(), config));
   }

   public synchronized static void writeValue(final PeASSFolders folders, final int average) throws JAXBException {
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
