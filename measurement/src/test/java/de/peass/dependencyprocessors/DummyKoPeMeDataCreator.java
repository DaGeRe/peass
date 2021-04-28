package de.peass.dependencyprocessors;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import de.dagere.kopeme.datacollection.DataCollector;
import de.dagere.kopeme.datacollection.TestResult;
import de.dagere.kopeme.datacollection.TimeDataCollector;
import de.dagere.kopeme.datacollection.tempfile.ResultTempWriter;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.Result.Fulldata.Value;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class DummyKoPeMeDataCreator {
   
   public static void initDummyTestfile(final File methodFolder, final int iterations, final TestCase testcase) throws JAXBException {
      XMLDataStorer storer = new XMLDataStorer(methodFolder, testcase.getClazz(), testcase.getMethod());

      final Result result = new Result();
      result.setValue(15);
      result.setIterations(iterations);
      initDummyFulldata(result, iterations);

      storer.storeValue(result, testcase.getExecutable(), TimeDataCollector.class.getName());
   }
   
   private static void initDummyFulldata(final Result result, final int count) {
      result.setFulldata(new Fulldata());
      final Value value = new Value();
      value.setValue(15);
      if (count > TestResult.BOUNDARY_SAVE_FILE) {
         try {
            ResultTempWriter writer = new ResultTempWriter(false);
            
            writeToDisk(count, writer);
            result.getFulldata().setFileName(writer.getTempFile().getAbsolutePath());
         } catch (IOException e) {
            e.printStackTrace();
         }
      } else {
         for (int i = 0; i < count; i++) {
            result.getFulldata().getValue().add(value);
         }
      }
   }

   private static void writeToDisk(final int count, final ResultTempWriter writer) {
      DataCollector[] collectors = new DataCollector[] {new DataCollector() {
         @Override
         public void stopCollection() {
         }
         
         @Override
         public void startCollection() {
         }
         
         @Override
         public long getValue() {
            return 15;
         }
         
         @Override
         public int getPriority() {
            return 0;
         }
         
         @Override
         public String getName() {
            return TimeDataCollector.class.getName();
         }
      }};
      writer.setDataCollectors(collectors);
      for (int i = 0; i < count; i++) {
         writer.executionStart(i);
         writer.writeValues(collectors);
      }
      writer.finalizeCollection();
   }
}
