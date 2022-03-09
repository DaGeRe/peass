package de.dagere.peass.measurement.dataloading;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
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

   public void loadData(PeassFolders folders, final TestCase testcase, final long currentChunkStart) throws JAXBException {
      final File kopemeFile = folders.getSummaryFile(testcase);
      final XMLDataLoader loader = new XMLDataLoader(kopemeFile);
      if (loader.getFullData().getTestcases().getTestcase().size() > 0) {
         final Datacollector dataCollector = loader.getFullData().getTestcases().getTestcase().get(0).getDatacollector().get(0);
         final Chunk realChunk = MultipleVMTestUtil.findChunk(currentChunkStart, dataCollector);
         loadChunk(realChunk);
      }
   }

   public void loadChunk(final Chunk realChunk) {
      LOG.debug("Chunk size: {}", realChunk.getResult().size());
      for (final Result result : realChunk.getResult()) {
         if (result.getIterations() + result.getWarmup() == config.getAllIterations() &&
               result.getRepetitions() == config.getRepetitions()) {
            if (result.getVersion().getGitversion().equals(config.getExecutionConfig().getVersionOld())) {
               before.add(result.getValue());
            }
            if (result.getVersion().getGitversion().equals(config.getExecutionConfig().getVersion())) {
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

   public static List<Result> removeResultsWithWrongConfiguration(final List<Result> results) {
      List<Result> cleaned = new LinkedList<>();
      long repetitions = MultipleVMTestUtil.getMinRepetitionCount(results);
      long iterations = MultipleVMTestUtil.getMinIterationCount(results);
      for (Result result : results) {
         if (repetitions == result.getRepetitions() &&
               iterations == result.getIterations()) {
            cleaned.add(result);
         }
      }

      return cleaned;
   }
}