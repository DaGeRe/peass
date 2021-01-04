package de.peass.measurement.analysis;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datacollection.TimeDataCollector;
import de.dagere.kopeme.datacollection.TimeDataCollectorNoGC;
import de.dagere.kopeme.datacollection.tempfile.WrittenResultReader;
import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.Result.Fulldata.Value;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.dagere.kopeme.generated.Versioninfo;
import de.peass.dependency.analysis.data.TestCase;

/**
 * Provides utilities for reading KoPeMe-data from multiple runs which should be summarized into one file.
 * 
 * @author reichelt
 *
 */
public class MultipleVMTestUtil {
   private static final Logger LOG = LogManager.getLogger(MultipleVMTestUtil.class);

   public static void main(final String[] args) throws JAXBException {
      final File resultFile = new File(args[0]);
      analyseOneRun(resultFile);
   }

   public static void analyseOneRun(final File resultFile) throws JAXBException {
      final DescriptiveStatistics st = getStatistics(resultFile);
      LOG.info("Durchschnitt: " + st.getMean());
   }

   public static DescriptiveStatistics getStatistics(final File resultFile) throws JAXBException {
      final XMLDataLoader fullDataLoader = new XMLDataLoader(resultFile);
      final Kopemedata fullResultData = fullDataLoader.getFullData();
      final Datacollector oneRunDatacollector = getTimeDataCollector(fullResultData);
      final DescriptiveStatistics st = new DescriptiveStatistics();
      for (final Result r : oneRunDatacollector.getResult()) {
         st.addValue(r.getValue());
      }
      return st;
   }

   public static Datacollector getTimeDataCollector(final Kopemedata fullResultData) {
      return getTimeDataCollector(fullResultData.getTestcases().getTestcase().get(0));
   }

   public static Datacollector getTimeDataCollector(final TestcaseType oneRunData) {
      Datacollector oneRunDatacollector = null;
      for (final Datacollector collector : oneRunData.getDatacollector()) {
         if (collector.getName().equals(TimeDataCollector.class.getName()) || collector.getName().equals(TimeDataCollectorNoGC.class.getName())) {
            oneRunDatacollector = collector;
         }
      }
      if (oneRunDatacollector == null) {
         throw new RuntimeException("Did not find " + TimeDataCollector.class.getName() + " or " + TimeDataCollectorNoGC.class);
      }
      return oneRunDatacollector;
   }

   /**
    * Takes the given result and the given version and creates a file containing the aggregated result.
    * 
    * @param summaryResultFile
    * @param oneRunData
    * @param version
    * @throws JAXBException
    */
   public static void saveSummaryData(final File summaryResultFile, final File oneResultFile, final TestcaseType oneRunData, final TestCase testcase, final String version, final long currentChunkStart)
         throws JAXBException {
      LOG.info("Writing to merged result file: {}", summaryResultFile);
      final Kopemedata summaryData = initKopemeData(summaryResultFile, testcase);
      Datacollector oneRunDatacollector = getTimeDataCollector(oneRunData);
      Chunk summaryChunk = findChunk(currentChunkStart, summaryData, oneRunDatacollector);

      final Result oneResult = oneRunDatacollector.getResult().get(0);
      if (oneResult.getFulldata().getFileName() != null) {
         SummaryStatistics st = getExternalFileStatistics(oneResultFile, oneRunDatacollector, oneResult);
         saveData(summaryResultFile, version, summaryData, summaryChunk, oneResult, st);
      } else {
         final Result cleaned = StatisticUtil.shortenResult(oneResult);
         final Fulldata realData = cleaned.getFulldata();
         if (realData != null && realData.getValue() != null && realData.getValue().size() > 0) {
            final SummaryStatistics st = createStatistics(realData);
            saveData(summaryResultFile, version, summaryData, summaryChunk, oneResult, st);
         } else {
            LOG.error("Fulldata of " + oneResultFile + " empty!");
         }
      }
   }

   private static void saveData(final File summaryResultFile, final String version, final Kopemedata summaryData, Chunk summaryChunk, final Result oneResult,
         final SummaryStatistics st) {
      final Result result = createResultFromStatistic(version, st, oneResult.getRepetitions());
      result.setDate(oneResult.getDate());
      result.setWarmup(oneResult.getWarmup());

      summaryChunk.getResult().add(result);
      XMLDataStorer.storeData(summaryResultFile, summaryData);
   }

   private static SummaryStatistics getExternalFileStatistics(final File oneResultFile, Datacollector oneRunDatacollector, final Result oneResult) {
      final File resultFile = new File(oneResultFile.getParentFile(), oneResult.getFulldata().getFileName());
      WrittenResultReader reader = new WrittenResultReader(resultFile);
      Set<String> keys = new HashSet<>();
      keys.add(oneRunDatacollector.getName());
      reader.read(null, keys);
      SummaryStatistics st = reader.getCollectorSummary(oneRunDatacollector.getName());
      return st;
   }

   public static Kopemedata initKopemeData(final File summaryResultFile, final TestCase testcase) throws JAXBException {
      final XMLDataLoader fullDataLoader = new XMLDataLoader(summaryResultFile);
      final Kopemedata fullResultData = fullDataLoader.getFullData();
      if (fullResultData.getTestcases().getTestcase().size() == 0) {
         fullResultData.getTestcases().setClazz(testcase.getClazz());
         fullResultData.getTestcases().getTestcase().add(new TestcaseType());
         fullResultData.getTestcases().getTestcase().get(0).setName(testcase.getMethod());
      }
      return fullResultData;
   }

   public static Chunk findChunk(final long currentChunkStart, final Kopemedata fullResultData, Datacollector oneRunDatacollector) {
      final List<Datacollector> fullResultFileDatacollectorList = fullResultData.getTestcases().getTestcase().get(0).getDatacollector();
      if (fullResultFileDatacollectorList.size() == 0) {
         fullResultFileDatacollectorList.add(new Datacollector());
         fullResultFileDatacollectorList.get(0).setName(oneRunDatacollector.getName());
      }
      final Datacollector fullFileDatacollector = fullResultFileDatacollectorList.get(0);
      Chunk realChunk = findChunk(currentChunkStart, fullFileDatacollector);
      if (realChunk == null) {
         realChunk = new Chunk();
         realChunk.setChunkStartTime(currentChunkStart);
         fullFileDatacollector.getChunk().add(realChunk);
      }
      return realChunk;
   }

   public static Chunk findChunk(final long currentChunkStart, final Datacollector fullFileDatacollector) {
      Chunk realChunk = null;
      for (final Chunk chunk : fullFileDatacollector.getChunk()) {
         if (chunk.getChunkStartTime() == currentChunkStart) {
            realChunk = chunk;
            break;
         }
      }
      return realChunk;
   }

   public static DescriptiveStatistics getChunkData(Chunk chunk, String version) {
      final DescriptiveStatistics desc1 = new DescriptiveStatistics();
      for (final Result result : chunk.getResult()) {
         if (result.getVersion().getGitversion().equals(version) && !Double.isNaN(result.getValue())) {
            desc1.addValue(result.getValue());
         }
      }
      return desc1;
   }

   public static long getMinExecutionCount(final List<Result> results) {
      long minExecutionTime = Long.MAX_VALUE;
      for (final Result result : results) {
         final long currentResultSize = result.getIterations();
         if (currentResultSize != 0) {
            minExecutionTime = Long.min(minExecutionTime, currentResultSize);
         }
      }
      return minExecutionTime;
   }

   private static Result createResultFromStatistic(final String version, final SummaryStatistics st, final long repetitions) {
      final Result result = new Result();
      result.setValue(st.getMean());
      result.setMin(st.getMin());
      result.setMax(st.getMax());
      result.setVersion(new Versioninfo());
      result.getVersion().setGitversion(version);
      result.setDeviation(st.getStandardDeviation());
      result.setIterations(st.getN());
      result.setRepetitions(repetitions);
      return result;
   }

   private static SummaryStatistics createStatistics(final Fulldata realData) {
      final SummaryStatistics st2 = new SummaryStatistics();
      final double[] values = new double[realData.getValue().size()];
      int i = 0;
      for (final Value value : realData.getValue()) {
         final long parseDouble =value.getValue();
         st2.addValue(parseDouble);
         values[i++] = parseDouble;
      }
      return st2;
   }

   public static List<Double> getAverages(final List<Result> before) {
      return before.stream()
            .mapToDouble(beforeVal -> beforeVal.getFulldata().getValue().stream()
                  .mapToDouble(val -> val.getValue()).sum()
                  / beforeVal.getFulldata().getValue().size())
            .boxed().sorted().collect(Collectors.toList());
   }

   public static SummaryStatistics getStatistic(final List<Result> results) {
      final SummaryStatistics statistisc = new SummaryStatistics();
      results.forEach(result -> statistisc.addValue(result.getValue()));
      return statistisc;
   }

   public static int compareDouble(final List<Double> before, final List<Double> after) {
      final boolean change = TestUtils.tTest(ArrayUtils.toPrimitive(before.toArray(new Double[0])), ArrayUtils.toPrimitive(after.toArray(new Double[0])), 0.05);
      final SummaryStatistics statisticBefore = new SummaryStatistics();
      before.forEach(result -> statisticBefore.addValue(result));

      final SummaryStatistics statisticAfter = new SummaryStatistics();
      after.forEach(result -> statisticAfter.addValue(result));
      if (change) {
         if (statisticBefore.getMean() < statisticAfter.getMean())
            return -1;
         else
            return 1;
      } else {
         return 0;
      }
   }

}
