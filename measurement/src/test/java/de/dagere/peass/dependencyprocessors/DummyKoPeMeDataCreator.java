package de.dagere.peass.dependencyprocessors;

import java.io.File;
import java.io.IOException;



import de.dagere.kopeme.datacollection.DataCollector;
import de.dagere.kopeme.datacollection.TestResult;
import de.dagere.kopeme.datacollection.TimeDataCollector;
import de.dagere.kopeme.datacollection.tempfile.ResultTempWriterBin;
import de.dagere.kopeme.datastorage.JSONDataStorer;
import de.dagere.kopeme.kopemedata.Fulldata;
import de.dagere.kopeme.kopemedata.MeasuredValue;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class DummyKoPeMeDataCreator {
   
   public static void initDummyTestfile(final File methodFolder, final int iterations, final TestCase testcase)  {
      JSONDataStorer storer = new JSONDataStorer(methodFolder, testcase.getClazz(), testcase.getMethod());

      final VMResult result = new VMResult();
      result.setValue(15);
      result.setIterations(iterations);
      initDummyFulldata(result, iterations);

      storer.storeValue(result, testcase.getExecutable(), TimeDataCollector.class.getName());
   }
   
   private static void initDummyFulldata(final VMResult result, final int count) {
      result.setFulldata(new Fulldata());
      final MeasuredValue value = new MeasuredValue();
      value.setValue(15);
      if (count > TestResult.BOUNDARY_SAVE_FILE) {
         try {
            ResultTempWriterBin writer = new ResultTempWriterBin(false);
            
            writeToDisk(count, writer);
            result.getFulldata().setFileName(writer.getTempFile().getAbsolutePath());
         } catch (IOException e) {
            e.printStackTrace();
         }
      } else {
         for (int i = 0; i < count; i++) {
            result.getFulldata().getValues().add(value);
         }
      }
   }

   private static void writeToDisk(final int count, final ResultTempWriterBin writer) {
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
