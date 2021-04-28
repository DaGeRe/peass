package de.dagere.peass.measurement.analysis;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.dagere.kopeme.generated.Versioninfo;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.measurement.analysis.statistics.EvaluationPair;
import de.dagere.peass.measurement.analysis.statistics.TestData;
import de.dagere.peass.statistics.StatisticUtil;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.utils.OptionConstants;

/**
 * Cleans measurement data by reading all iteration-values of every VM, dividing them in the middle and saving the results in a clean-folder in single chunk-entries in a
 * measurement file for each test method.
 * 
 * This makes it possible to process the data faster, e.g. for determining performance changes or statistic analysis.
 * 
 * @author reichelt
 *
 */
public class Cleaner extends DataAnalyser  {

   private static final Logger LOG = LogManager.getLogger(Cleaner.class);

   private final File measurementsFull;
   private int correct = 0;
   protected int read = 0;

   public int getCorrect() {
      return correct;
   }

   public int getRead() {
      return read;
   }

   public Cleaner(final File measurementsFull) {
      this.measurementsFull = measurementsFull;
      if (measurementsFull.exists()) {
         throw new RuntimeException("Clean already finished - delete " + measurementsFull.getAbsolutePath() + ", if you want to clean!");
      } else {
         measurementsFull.mkdirs();
      }
   }

   @Override
   public void processTestdata(final TestData measurementEntry) {
      for (final Entry<String, EvaluationPair> entry : measurementEntry.getMeasurements().entrySet()) {
         read++;
         cleanTestVersionPair(entry);
      }
   }

   public void cleanTestVersionPair(final Entry<String, EvaluationPair> entry) {
      final String clazz = entry.getValue().getTestcase().getClazz();
      final String method = entry.getValue().getTestcase().getMethod();
      if (entry.getValue().getPrevius().size() >= 2 && entry.getValue().getCurrent().size() >= 2) {
         final Chunk currentChunk = new Chunk();
         final long minExecutionCount = MultipleVMTestUtil.getMinIterationCount(entry.getValue().getPrevius());

         final List<Result> previous = getChunk(entry.getValue().getPreviousVersion(), minExecutionCount, entry.getValue().getPrevius());
         currentChunk.getResult().addAll(previous);

         final List<Result> current = getChunk(entry.getValue().getVersion(), minExecutionCount, entry.getValue().getCurrent());
         currentChunk.getResult().addAll(current);

         handleChunk(entry, clazz, method, currentChunk);
      }
   }

   private void handleChunk(final Entry<String, EvaluationPair> entry, final String clazz, final String method, final Chunk currentChunk) {
      try {
         final MeasurementFileFinder finder = new MeasurementFileFinder(measurementsFull, clazz, method);
         final File measurementFile = finder.getMeasurementFile();
         final Kopemedata oneResultData = finder.getOneResultData();
         Datacollector datacollector = finder.getDataCollector();

         if (checkChunk(currentChunk)) {
            datacollector.getChunk().add(currentChunk);
            XMLDataStorer.storeData(measurementFile, oneResultData);
            correct++;
         } else {
            printFailureInfo(entry, currentChunk, measurementFile);
         }
      } catch (final JAXBException e) {
         e.printStackTrace();
      }
   }

   private void printFailureInfo(final Entry<String, EvaluationPair> entry, final Chunk currentChunk, final File measurementFile) {
      for (final Result r : entry.getValue().getPrevius()) {
         LOG.debug("Value: {} Executions: {} Repetitions: {}", r.getValue(), r.getIterations(), r.getRepetitions());
      }
      for (final Result r : entry.getValue().getCurrent()) {
         LOG.debug("Value:  {} Executions: {} Repetitions: {}", r.getValue(), r.getIterations(), r.getRepetitions());
      }
      LOG.debug("Too few correct measurements: {} ", measurementFile.getAbsolutePath());
      LOG.debug("Measurements: {} / {}", currentChunk.getResult().size(), entry.getValue().getPrevius().size() + entry.getValue().getCurrent().size());
   }

   public boolean checkChunk(final Chunk currentChunk) {
      return currentChunk.getResult().size() > 2;
   }

   private static final long ceilDiv(final long x, final long y) {
      return -Math.floorDiv(-x, y);
   }

   private List<Result> getChunk(final String version, final long minExecutionCount, final List<Result> previous) {
      final List<Result> previousClean = StatisticUtil.shortenValues(previous);
      return previousClean.stream()
            .filter(result -> {
               final int resultSize = result.getFulldata().getValue().size();
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

   private Result cleanResult(final String version, final Result result) {
      result.setVersion(new Versioninfo());
      result.getVersion().setGitversion(version);
      result.setWarmup(result.getFulldata().getValue().size());
      result.setIterations(result.getFulldata().getValue().size());
      result.setRepetitions(result.getRepetitions());
      result.setMin(null);
      result.setMax(null);
      result.setFulldata(new Fulldata());
      return result;
   }

   public static void loadDependencies(final CommandLine line) throws JsonParseException, JsonMappingException, IOException {
      if (line.hasOption(OptionConstants.DEPENDENCYFILE.getName())) {
         final File dependencyFile = new File(line.getOptionValue(OptionConstants.DEPENDENCYFILE.getName()));
         final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
         VersionComparator.setDependencies(dependencies);
      } else {
         LOG.error("No dependencyfile information passed.");
         throw new RuntimeException("No dependencyfile information passed.");
      }
   }
}
