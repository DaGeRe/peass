package de.dagere.peass.breaksearch;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;

import de.dagere.kopeme.kopemedata.MeasuredValue;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.analysis.measurement.TestStatistic;
import de.dagere.peass.measurement.dataloading.DataAnalyser;
import de.dagere.peass.measurement.dataloading.DataReader;
import de.dagere.peass.measurement.statistics.data.EvaluationPair;
import de.dagere.peass.measurement.statistics.data.TestData;

public class IsThereTimeReductionIterations extends DataAnalyser {
   final static int CHUNK_SIZE = 100;
   final static int CHUNK_COUNT = 5;

   static int additionalFound = 0;
   static int lessfound = 0, speedup = 0;
   static int count = 0;
   static int komisch = 0;

   static long avgcount = 0;
   static int vms = 0;
   
   public void analyze(final File[] data) throws InterruptedException {
      for (File folder : data) {
         for (final File slaveFolder : folder.listFiles()) {
            final File fullDataFolder = new File(slaveFolder, "measurementsFull/measurements/");
            final LinkedBlockingQueue<TestData> measurements = new LinkedBlockingQueue<>();
            DataReader.startReadVersionDataMap(fullDataFolder, measurements);

            TestData measurementEntry = measurements.take();
            while (measurementEntry != DataReader.POISON_PILL) {
               try {
                  System.out.println("Analyze: " + measurementEntry.getTestClass());
                  processTestdata(measurementEntry);
               } catch (final RuntimeException e) {

               }
               measurementEntry = measurements.take();
            }

         }
      }

      System.out.println("Additional: " + additionalFound + " Wrong: " + lessfound + " Speedup:" + speedup + " Tests: " + count);
      System.out.println("Average Iterations: " + avgcount / vms);
   }
   
   @Override
   public void processTestdata(final TestData measurementEntry) {
      for (final Entry<String, EvaluationPair> entry : measurementEntry.getMeasurements().entrySet()) {
         final boolean isChange = new TestStatistic(entry.getValue()).isChange();
         final String version = entry.getKey();
         count++;
         System.out.println("Analyze: " + entry.getValue().getTestcase());
         final List<double[]> beforeMeasurements = getMeasurements(entry.getValue().getPrevius());
         final List<double[]> afterMeasurements = getMeasurements(entry.getValue().getCurrent());

         // final List<double[]> shortenedMeasurements = new LinkedList<>();
         final double[] valsBefore = getShortenedValues(beforeMeasurements);
         final double[] valsAfter = getShortenedValues(afterMeasurements);

         final boolean tNew = TestUtils.tTest(valsBefore, valsAfter, 0.01);

         if (!isChange && tNew) {
            additionalFound++;
         }

         if (isChange == tNew) {
            System.out.println("Works!");
            if (isChange) {
               speedup++;
               // additionalFound++;
            }
         } else {
            lessfound++;
            System.out.println("Wrong: " + version + " " + entry.getValue().getVersion());
         }
      }
   }

   private List<double[]> getMeasurements(final List<VMResult> previusValues) {
      final List<double[]> beforeMeasurements = new LinkedList<>();
      for (final VMResult result : previusValues) {
         final double[] vals = new double[result.getFulldata().getValues().size()];
         int index = 0;
         for (final MeasuredValue value : result.getFulldata().getValues()) {
            vals[index] = value.getValue();
            index++;
         }
         beforeMeasurements.add(vals);
      }
      return beforeMeasurements;
   }
   
   private static double[] getShortenedValues(final List<double[]> beforeMeasurements) {
      final double[] valsBefore = new double[beforeMeasurements.size()];
      int index = 0;
      for (final double[] values : beforeMeasurements) {
         final int breakcount = getBreakCount(values);
         if (values.length == 1000) {
            avgcount += breakcount;
            vms++;
         }
         System.out.println("Break: " + breakcount);
         final double[] shortened = new double[breakcount];
         System.arraycopy(values, 0, shortened, 0, breakcount);
         valsBefore[index] = new DescriptiveStatistics(shortened).getMean();
         index++;
      }
      return valsBefore;
   }

   private static int getBreakCount(final double[] values) {
      int breakcount = values.length;
      for (int i = CHUNK_SIZE * CHUNK_COUNT; i < values.length - CHUNK_SIZE * CHUNK_COUNT; i += CHUNK_SIZE) {
         final List<DescriptiveStatistics> chunks = new LinkedList<>();
         final double[] meanDeviations = new double[CHUNK_COUNT];
         for (int chunk = 0; chunk < CHUNK_COUNT; chunk++) {
            final double[] lastChunk = new double[CHUNK_SIZE];
            // System.out.println("Values: " + values.length + " " + (i - CHUNK_SIZE * chunk));
            System.arraycopy(values, i - CHUNK_SIZE * chunk, lastChunk, 0, CHUNK_SIZE);
            final DescriptiveStatistics descriptiveStatistics = new DescriptiveStatistics(lastChunk);
            chunks.add(descriptiveStatistics);
            meanDeviations[chunk] = descriptiveStatistics.getMean();
         }

         // System.out.println(Arrays.toString(meanDeviations));
         final DescriptiveStatistics overall = new DescriptiveStatistics(meanDeviations);
         final double relativeDeviation = overall.getStandardDeviation() / overall.getMean();
         // System.out.println("I: " + i + " Mean: " + overall.getMean() + " " + relativeDeviation);
         // System.out.println(relativeDeviation);
         if (relativeDeviation < 0.01 && i > 10000) {
            // System.out.println("Break in: " + i);
            breakcount = i;
            break;
         }
      }
      return breakcount;
   }
}
