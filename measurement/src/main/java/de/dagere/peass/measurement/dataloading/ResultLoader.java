package de.dagere.peass.measurement.dataloading;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.kopeme.kopemedata.VMResultChunk;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.folders.PeassFolders;


public class ResultLoader {

   private static final Logger LOG = LogManager.getLogger(ResultLoader.class);

   private final MeasurementConfig config;

   private final List<Double> before = new LinkedList<>();
   private final List<Double> after = new LinkedList<>();

   public ResultLoader(final MeasurementConfig config) {
      this.config = config;
   }

   public void loadData(PeassFolders folders, final TestCase testcase, final long currentChunkStart)  {
      final File kopemeFile = folders.getSummaryFile(testcase);
      final Kopemedata data = JSONDataLoader.loadData(kopemeFile);
      if (data.getMethods().size() > 0) {
         final DatacollectorResult dataCollector = data.getMethods().get(0).getDatacollectorResults().get(0);
         final VMResultChunk realChunk = MultipleVMTestUtil.findChunk(currentChunkStart, dataCollector);
         loadChunk(realChunk);
      }
   }

   public void loadChunk(final VMResultChunk realChunk) {
      LOG.debug("Chunk size: {}", realChunk.getResults().size());
      for (final VMResult result : realChunk.getResults()) {
         if (result.getIterations() + result.getWarmup() == config.getAllIterations() &&
               result.getRepetitions() == config.getRepetitions()) {
            if (result.getCommit().equals(config.getExecutionConfig().getCommitOld())) {
               before.add(result.getValue());
            }
            if (result.getCommit().equals(config.getExecutionConfig().getCommit())) {
               after.add(result.getValue());
            }
         }
      }
   }

   public DescriptiveStatistics getStatisticsBefore() {
      return new DescriptiveStatistics(getValsBefore());
   }

   public DescriptiveStatistics getStatisticsAfter() {
      return new DescriptiveStatistics(getValsAfter());
   }

   public double[] getValsBefore() {
      return ArrayUtils.toPrimitive(before.toArray(new Double[0]));
   }

   public double[] getValsAfter() {
      return ArrayUtils.toPrimitive(after.toArray(new Double[0]));
   }

   public static List<VMResult> removeResultsWithWrongConfiguration(final List<VMResult> results) {
      List<VMResult> cleaned = new LinkedList<>();
      long repetitions = MultipleVMTestUtil.getMinRepetitionCount(results);
      long iterations = MultipleVMTestUtil.getMinIterationCount(results);
      for (VMResult result : results) {
         if (repetitions == result.getRepetitions() &&
               iterations == result.getIterations()) {
            cleaned.add(result);
         }
      }

      return cleaned;
   }
}