package de.dagere.peass.measurement.dataloading;

import java.io.File;
import java.io.FileFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.statistics.data.TestData;

/**
 * Reads measurement data sequentially, reading files after each other to a queue and stops reading if the queue gets too full (currently 10 elements).
 * 
 * @author reichelt
 *
 */
public final class DataReader {
   private static final int MAX_QUEUE_SIZE = 25;

   private static final Logger LOG = LogManager.getLogger(DataReader.class);

   public static final TestData POISON_PILL = new TestData(null, null);
   private static int size = 0;

   private DataReader() {

   }

   public static Thread startReadVersionDataMap(final File fullDataFolder, LinkedBlockingQueue<TestData> myQueue) {
      final Thread readerThread = new Thread(new Runnable() {

         @Override
         public void run() {
            size = 0;
            LOG.debug("Starting data-reading from: {}", fullDataFolder);
            readDataToQueue(fullDataFolder, myQueue);
            myQueue.add(POISON_PILL);
            LOG.debug("Finished data-reading, testcase-changes: {}", size);
         }
      });
      readerThread.start();

      return readerThread;
   }

   private static void readDataToQueue(final File fullDataFolder, final LinkedBlockingQueue<TestData> measurements) {
      LOG.info("Loading folder: {}", fullDataFolder);

      for (final File clazzFile : fullDataFolder.listFiles()) {
         final Map<String, TestData> currentMeasurement = readClassFolder(clazzFile);

         for (final TestData data : currentMeasurement.values()) {
            LOG.debug("Add: {}", data.getTestClass() + " " + data.getTestMethod());
            while (measurements.size() > MAX_QUEUE_SIZE) {
               LOG.info("Waiting, Measurements: {} Max-Queue-Size: {}", measurements.size(), MAX_QUEUE_SIZE);
               try {
                  Thread.sleep(1000);
               } catch (final InterruptedException e) {
                  e.printStackTrace();
               }
            }
            measurements.add(data);
            size += data.getVersions();
         }
      }
   }

   public static Map<String, TestData> readClassFolder(final File clazzFile) {
      final Map<String, TestData> currentMeasurement = new HashMap<>();
      for (final File versionOfPair : clazzFile.listFiles()) {
         if (versionOfPair.isDirectory()) {
            for (final File versionCurrent : versionOfPair.listFiles()) {
               for (final File measurementFile : versionCurrent.listFiles((FileFilter) new WildcardFileFilter("*.json"))) {
                  readMeasurementFile(currentMeasurement, versionOfPair, versionCurrent, measurementFile);
               }
               
               // For compatibility with reading old xml result data, this needs to stay in the code
               for (final File measurementFile : versionCurrent.listFiles((FileFilter) new WildcardFileFilter("*.xml"))) {
                  readMeasurementFile(currentMeasurement, versionOfPair, versionCurrent, measurementFile);
               }
            }
         } else {
            LOG.error("Version-folder does not exist: {}", versionOfPair.getAbsolutePath());
         }
      }
      return currentMeasurement;
   }

   private static void readMeasurementFile(final Map<String, TestData> currentMeasurement, final File versionOfPair, final File versionCurrent, final File measurementFile) {
      final Kopemedata resultData = JSONDataLoader.loadData(measurementFile);
      final String testclazz = resultData.getClazz();
      TestCase testcase = new TestCase(resultData);
      TestData testData = currentMeasurement.get(testcase.getMethodWithParams());
      if (testData == null) {
         final File originFile = measurementFile.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
         testData = new TestData(testcase, originFile);
         currentMeasurement.put(testcase.getMethodWithParams(), testData);
      }

      String predecessor = null;
      final File versionFiles[] = versionOfPair.listFiles();
      for (final File version : versionFiles) {
         if (!version.getName().equals(versionOfPair.getName())) {
            predecessor = version.getName();
         }
      }

      if (predecessor != null) {
         testData.addMeasurement(versionOfPair.getName(), versionCurrent.getName(), predecessor, resultData);
      } else {
         LOG.error("No predecessor data for {} {} {} {}", versionCurrent.getName(), predecessor, testclazz, testcase.getMethodWithParams());
      }

   }
}
