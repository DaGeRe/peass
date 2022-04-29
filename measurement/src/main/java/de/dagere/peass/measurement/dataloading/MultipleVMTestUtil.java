package de.dagere.peass.measurement.dataloading;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datacollection.tempfile.WrittenResultReaderBin;
import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.datastorage.JSONDataStorer;
import de.dagere.kopeme.kopemedata.DatacollectorResult;
import de.dagere.kopeme.kopemedata.Fulldata;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.MeasuredValue;
import de.dagere.kopeme.kopemedata.TestMethod;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.kopeme.kopemedata.VMResultChunk;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.statistics.StatisticUtil;

/**
 * Provides utilities for reading KoPeMe-data from multiple runs which should be summarized into one file.
 * 
 * @author reichelt
 *
 */
public class MultipleVMTestUtil {
   private static final Logger LOG = LogManager.getLogger(MultipleVMTestUtil.class);

   public static void main(final String[] args) {
      final File resultFile = new File(args[0]);
      analyseOneRun(resultFile);
   }

   public static void analyseOneRun(final File resultFile) {
      final DescriptiveStatistics st = getStatistics(resultFile);
      LOG.info("Durchschnitt: " + st.getMean());
   }

   public static DescriptiveStatistics getStatistics(final File resultFile) {
      final Kopemedata fullResultData = JSONDataLoader.loadData(resultFile);
            
      final DatacollectorResult oneRunDatacollector = fullResultData.getFirstTimeDataCollector();
      final DescriptiveStatistics st = new DescriptiveStatistics();
      for (final VMResult r : oneRunDatacollector.getResults()) {
         st.addValue(r.getValue());
      }
      return st;
   }

   /**
    * Takes the given result and the given version and creates a file containing the aggregated result.
    * 
    * @param summaryResultFile
    * @param oneRunData
    * @param version
    * @
    */
   public static void saveSummaryData(final File summaryResultFile, final File oneResultFile, final VMResult oneResult, final TestCase testcase, final String version,
         final long currentChunkStart, final String datacollectorName) {
      LOG.info("Writing to merged result file: {}", summaryResultFile);
      final Kopemedata summaryData = initKopemeData(summaryResultFile, testcase);
      VMResultChunk summaryChunk = findChunk(currentChunkStart, summaryData, datacollectorName);
      
      if (oneResult.getFulldata().getFileName() != null) {
         SummaryStatistics st = getExternalFileStatistics(oneResultFile, datacollectorName, oneResult);
         saveData(summaryResultFile, version, summaryData, summaryChunk, oneResult, st);
      } else {
         final VMResult cleaned;
         if (oneResult.getWarmup() != 0) {
            cleaned = StatisticUtil.shortenResult(oneResult);
         } else {
            cleaned = oneResult;
         }
         final Fulldata realData = cleaned.getFulldata();
         if (realData != null && realData.getValues() != null && realData.getValues().size() > 0) {
            final SummaryStatistics st = createStatistics(realData);
            saveData(summaryResultFile, version, summaryData, summaryChunk, oneResult, st);
         } else {
            LOG.error("Fulldata of " + oneResultFile + " empty!");
         }
      }
   }

   private static void saveData(final File summaryResultFile, final String version, final Kopemedata summaryData, final VMResultChunk summaryChunk, final VMResult oneResult,
         final SummaryStatistics st) {
      final VMResult result = createResultFromStatistic(version, st, oneResult.getRepetitions());
      result.setDate(oneResult.getDate());
      result.setWarmup(oneResult.getWarmup());
      result.setParameters(oneResult.getParameters());

      summaryChunk.getResults().add(result);
      JSONDataStorer.storeData(summaryResultFile, summaryData);
   }

   private static SummaryStatistics getExternalFileStatistics(final File oneResultFile, final String dataCollectorName, final VMResult oneResult) {
      final File resultFile = new File(oneResultFile.getParentFile(), oneResult.getFulldata().getFileName());
      WrittenResultReaderBin reader = new WrittenResultReaderBin(resultFile);
      Set<String> keys = new HashSet<>();
      keys.add(dataCollectorName);
      reader.read(null, keys);
      SummaryStatistics st = reader.getCollectorSummary(dataCollectorName);
      return st;
   }

   public static Kopemedata initKopemeData(final File summaryResultFile, final TestCase testcase) {
      final Kopemedata fullResultData = JSONDataLoader.loadData(summaryResultFile);
      if (fullResultData.getMethods().size() == 0) {
         fullResultData.setClazz(testcase.getClassWithModule());
         fullResultData.getMethods().add(new TestMethod(testcase.getMethod()));
      }
      return fullResultData;
   }

   public static VMResultChunk findChunk(final long currentChunkStart, final Kopemedata fullResultData, final String datacollectorName) {
      final List<DatacollectorResult> fullResultFileDatacollectorList = fullResultData.getFirstMethodResult().getDatacollectorResults();
      if (fullResultFileDatacollectorList.size() == 0) {
         fullResultFileDatacollectorList.add(new DatacollectorResult(datacollectorName));
      }
      final DatacollectorResult fullFileDatacollector = fullResultFileDatacollectorList.get(0);
      VMResultChunk realChunk = findChunk(currentChunkStart, fullFileDatacollector);
      if (realChunk == null) {
         realChunk = new VMResultChunk();
         realChunk.setChunkStartTime(currentChunkStart);
         fullFileDatacollector.getChunks().add(realChunk);
      }
      return realChunk;
   }

   public static VMResultChunk findChunk(final long currentChunkStart, final DatacollectorResult fullFileDatacollector) {
      VMResultChunk realChunk = null;
      for (final VMResultChunk chunk : fullFileDatacollector.getChunks()) {
         if (chunk.getChunkStartTime() == currentChunkStart) {
            realChunk = chunk;
            break;
         }
      }
      return realChunk;
   }

   public static DescriptiveStatistics getChunkData(final VMResultChunk chunk, final String version) {
      final DescriptiveStatistics desc1 = new DescriptiveStatistics();
      for (final VMResult result : chunk.getResults()) {
         if (result.getCommit().equals(version) && !Double.isNaN(result.getValue())) {
            desc1.addValue(result.getValue());
         }
      }
      return desc1;
   }
   
   public static long getMinRepetitionCount(final List<VMResult> results) {
      long minRepetitions = Long.MAX_VALUE;
      long minMultiplied = Long.MAX_VALUE;
      for (final VMResult result : results) {
         final long currentIterations = result.getIterations();
         final long currentRepetitions = result.getRepetitions();
         long multiplied = currentIterations * currentRepetitions;
         if (multiplied != 0) {
            if (multiplied < minMultiplied) {
               minRepetitions = currentRepetitions;
               minMultiplied = multiplied;
            }
         }
      }
      return minRepetitions;
   }

   public static long getMinIterationCount(final List<VMResult> results) {
      long minIterations = Long.MAX_VALUE;
      long minMultiplied = Long.MAX_VALUE;
      for (final VMResult result : results) {
         final long currentIterations = result.getIterations();
         final long currentRepetitions = result.getRepetitions();
         long multiplied = currentIterations * currentRepetitions;
         if (multiplied != 0) {
            if (multiplied < minMultiplied) {
               minIterations = currentIterations;
               minMultiplied = multiplied;
            }
         }
      }
      return minIterations;
   }

   private static VMResult createResultFromStatistic(final String version, final SummaryStatistics st, final long repetitions) {
      final VMResult result = new VMResult();
      result.setValue(st.getMean());
      result.setMin(st.getMin());
      result.setMax(st.getMax());
      result.setCommit(version);
      result.setDeviation(st.getStandardDeviation());
      result.setIterations(st.getN());
      result.setRepetitions(repetitions);
      return result;
   }

   private static SummaryStatistics createStatistics(final Fulldata realData) {
      final SummaryStatistics st2 = new SummaryStatistics();
      final double[] values = new double[realData.getValues().size()];
      int i = 0;
      for (final MeasuredValue value : realData.getValues()) {
         final long parseDouble = value.getValue();
         st2.addValue(parseDouble);
         values[i++] = parseDouble;
      }
      return st2;
   }

   public static List<Double> getAverages(final List<VMResult> before) {
      return before.stream()
            .mapToDouble(beforeVal -> beforeVal.getFulldata().getValues().stream()
                  .mapToDouble(val -> val.getValue()).sum()
                  / beforeVal.getFulldata().getValues().size())
            .boxed().sorted().collect(Collectors.toList());
   }

   public static SummaryStatistics getStatistic(final List<VMResult> results) {
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
