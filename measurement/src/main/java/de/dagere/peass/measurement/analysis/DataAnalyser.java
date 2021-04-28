package de.dagere.peass.measurement.analysis;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.measurement.analysis.statistics.TestData;

public abstract class DataAnalyser {
   private static final Logger LOG = LogManager.getLogger(DataAnalyser.class);

   public void analyseFolder(final File measurementsFolder) throws InterruptedException {
      LOG.info("Loading: {}", measurementsFolder);

      if (!measurementsFolder.exists()) {
         LOG.error("Ordner existiert nicht!");
         System.exit(1);
      }

      final LinkedBlockingQueue<TestData> measurements = DataReader.startReadVersionDataMap(measurementsFolder);

      TestData measurementEntry = measurements.take();

      while (measurementEntry != DataReader.POISON_PILL) {
         try {
            processTestdata(measurementEntry);
         } catch (final RuntimeException e) {
            e.printStackTrace();
            // Show exception, but continue - exception may be
            // caused by long-running testcases..
         }
         // LOG.info("Taking.." + measurements.size());
         measurementEntry = measurements.take();
      }
      // LOG.info("Finished");
   }

   public abstract void processTestdata(TestData measurementEntry);

   /**
    * Process a found folder, i.e. a folder containing measurements.
    * 
    * @param folder Folder to process
    */
   public void processDataFolder(final File folder) {
      for (final File measurementFolder : folder.listFiles()) {
         if (measurementFolder.isDirectory() && !measurementFolder.getName().equals("ignore") && !measurementFolder.getName().equals("clean")) {
            if (measurementFolder.getName().equals("measurements")) {
               LOG.info("Analysing: {}", measurementFolder.getAbsolutePath());
               try {
                  analyseFolder(measurementFolder);
               } catch (final InterruptedException e) {
                  e.printStackTrace();
               }
            } else {
               processDataFolder(measurementFolder);
            }
         }
      }
   }
}
