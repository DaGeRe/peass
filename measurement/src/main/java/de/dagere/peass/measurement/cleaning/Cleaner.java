package de.dagere.peass.measurement.cleaning;

import java.io.File;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.JSONDataStorer;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Fulldata;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.kopeme.kopemedata.VMResultChunk;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.dataloading.DataAnalyser;
import de.dagere.peass.measurement.dataloading.MeasurementFileFinder;
import de.dagere.peass.measurement.dataloading.MultipleVMTestUtil;
import de.dagere.peass.measurement.statistics.StatisticUtil;
import de.dagere.peass.measurement.statistics.data.EvaluationPair;
import de.dagere.peass.measurement.statistics.data.TestData;

/**
 * Cleans measurement data by reading all iteration-values of every VM, dividing them in the middle and saving the results in a clean-folder in single chunk-entries in a
 * measurement file for each test method.
 * 
 * This makes it possible to process the data faster, e.g. for determining performance changes or statistic analysis.
 * 
 * @author reichelt
 *
 */
public class Cleaner extends DataAnalyser {

   private static final Logger LOG = LogManager.getLogger(Cleaner.class);

   private final File cleanFolder;
   private int correct = 0;
   protected int read = 0;

   public int getCorrect() {
      return correct;
   }

   public int getRead() {
      return read;
   }

   public Cleaner(final File cleanFolder) {
      this.cleanFolder = cleanFolder;
   }

   @Override
   public void processTestdata(final TestData measurementEntry) {
      for (final Entry<String, EvaluationPair> entry : measurementEntry.getMeasurements().entrySet()) {
         read++;
         cleanTestVersionPair(entry);
      }
   }

   public void cleanTestVersionPair(final Entry<String, EvaluationPair> entry) {
      TestCase testcase = entry.getValue().getTestcase();
      if (entry.getValue().getPrevius().size() >= 2 && entry.getValue().getCurrent().size() >= 2) {
         final VMResultChunk currentChunk = new VMResultChunk();
         final long minExecutionCount = MultipleVMTestUtil.getMinIterationCount(entry.getValue().getPrevius());

         final List<VMResult> previous = getChunk(entry.getValue().getPreviousVersion(), minExecutionCount, entry.getValue().getPrevius());
         currentChunk.getResults().addAll(previous);

         final List<VMResult> current = getChunk(entry.getValue().getVersion(), minExecutionCount, entry.getValue().getCurrent());
         currentChunk.getResults().addAll(current);

         handleChunk(entry, testcase, currentChunk);
      }
   }

   private void handleChunk(final Entry<String, EvaluationPair> entry, TestCase testcase, final VMResultChunk currentChunk) {
      final MeasurementFileFinder finder = new MeasurementFileFinder(cleanFolder, testcase);
      final File measurementFile = finder.getMeasurementFile();
      final Kopemedata oneResultData = finder.getOneResultData();
      DatacollectorResult datacollector = finder.getDataCollector();

      if (checkChunk(currentChunk)) {
         datacollector.getChunks().add(currentChunk);
         JSONDataStorer.storeData(measurementFile, oneResultData);
         correct++;
      } else {
         printFailureInfo(entry, currentChunk, measurementFile);
      }
   }

   private void printFailureInfo(final Entry<String, EvaluationPair> entry, final VMResultChunk currentChunk, final File measurementFile) {
      for (final VMResult r : entry.getValue().getPrevius()) {
         LOG.debug("Value: {} Executions: {} Repetitions: {}", r.getValue(), r.getIterations(), r.getRepetitions());
      }
      for (final VMResult r : entry.getValue().getCurrent()) {
         LOG.debug("Value:  {} Executions: {} Repetitions: {}", r.getValue(), r.getIterations(), r.getRepetitions());
      }
      LOG.debug("Too few correct measurements: {} ", measurementFile.getAbsolutePath());
      LOG.debug("Measurements: {} / {}", currentChunk.getResults().size(), entry.getValue().getPrevius().size() + entry.getValue().getCurrent().size());
   }

   public boolean checkChunk(final VMResultChunk currentChunk) {
      return currentChunk.getResults().size() > 2;
   }

   private static final long ceilDiv(final long x, final long y) {
      return -Math.floorDiv(-x, y);
   }

   private List<VMResult> getChunk(final String version, final long minExecutionCount, final List<VMResult> previous) {
      final List<VMResult> previousClean = StatisticUtil.shortenValues(previous);
      return previousClean.stream()
            .filter(result -> {
               final int resultSize = result.getFulldata().getValues().size();
               final long expectedSize = ceilDiv(minExecutionCount, 2);
               final boolean isCorrect = resultSize == expectedSize && !Double.isNaN(result.getValue());
               if (!isCorrect) {
                  LOG.debug("Wrong size: {} Expected: {}", resultSize, expectedSize);
               }
               return isCorrect;
            })
            .map(result -> cleanResult(version, result))
            .collect(Collectors.toList());
   }

   private VMResult cleanResult(final String version, final VMResult result) {
      result.setCommit(version);
      result.setWarmup(result.getFulldata().getValues().size());
      result.setIterations(result.getFulldata().getValues().size());
      result.setRepetitions(result.getRepetitions());
      result.setMin(null);
      result.setMax(null);
      result.setFulldata(new Fulldata());
      return result;
   }
}
