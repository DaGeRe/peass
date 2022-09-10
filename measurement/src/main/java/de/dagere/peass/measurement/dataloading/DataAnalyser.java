package de.dagere.peass.measurement.dataloading;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.measurement.statistics.data.TestData;

public abstract class DataAnalyser {
   
   private static final Logger LOG = LogManager.getLogger(DataAnalyser.class);
   
   private boolean isFinished = false;
   protected final CommitComparatorInstance comparator;

   public DataAnalyser(CommitComparatorInstance comparator) {
      this.comparator = comparator;
   }
   
   public void analyseFolder(final File measurementsFolder) throws InterruptedException {
      LOG.info("Loading: {}", measurementsFolder);

      if (!measurementsFolder.exists()) {
         LOG.error("Folder not existing: {}", measurementsFolder);
         System.exit(1);
      }

      final LinkedBlockingQueue<TestData> measurements = new LinkedBlockingQueue<>();
      final Thread readerThread = DataReader.startReadVersionDataMap(measurementsFolder, measurements, comparator);

      Thread processorThread = new Thread(new Runnable() {
         
         @Override
         public void run() {
            isFinished = false;
            TestData measurementEntry;
            try {
               measurementEntry = measurements.take();
               
               while (measurementEntry != DataReader.POISON_PILL && !isFinished) {
                  processTestdata(measurementEntry);
                  measurementEntry = measurements.take();
               }
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
         }
      });
      processorThread.start();
      readerThread.join();
      
      finishProcessingIfRunning(processorThread);
   }

   private void finishProcessingIfRunning(Thread processorThread) throws InterruptedException {
      Thread.sleep(100);
      isFinished = true;
      processorThread.join();
//      processorThread.interrupt();
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
