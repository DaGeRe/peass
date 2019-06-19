package de.peass.measurement.analysis;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import javax.xml.bind.JAXBException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.groovy.util.StringUtil;

import de.dagere.kopeme.datacollection.DataCollector;
import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.dagere.kopeme.generated.Versioninfo;
import de.peass.dependency.reader.DependencyReaderUtil;
import de.peass.measurement.analysis.statistics.EvaluationPair;
import de.peass.measurement.analysis.statistics.TestData;
import de.peass.statistics.ConfidenceInterval;
import de.peass.utils.OptionConstants;

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
   
   public static void main(final String[] args) throws ParseException, JAXBException {
      final Options options = OptionConstants.createOptions(OptionConstants.DEPENDENCYFILE, OptionConstants.DATA);

      final CommandLineParser parser = new DefaultParser();
      final CommandLine line = parser.parse(options, args);

      DependencyReaderUtil.loadDependencies(line);

      for (int i = 0; i < line.getOptionValues(OptionConstants.DATA.getName()).length; i++) {
         final File folder = new File(line.getOptionValues(OptionConstants.DATA.getName())[i]);
         LOG.info("Searching in " + folder);
         final File cleanFolder = new File(folder.getParentFile(), "clean");
         cleanFolder.mkdirs();
         final File sameNameFolder = new File(cleanFolder, folder.getName());
         sameNameFolder.mkdirs();
         final File fulldataFolder = new File(sameNameFolder, "measurementsFull");
         fulldataFolder.mkdirs();
         final Cleaner transformer = new Cleaner(fulldataFolder);
         LOG.info("Start");
         transformer.processDataFolder(folder);
         LOG.info("Finish");
      }
   }

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
         // try {
         // FileUtils.deleteDirectory(measurementsFull);
         // } catch (IOException e) {
         // e.printStackTrace();
         // }
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
         final long minExecutionCount = MultipleVMTestUtil.getMinExecutionCount(entry.getValue().getPrevius());

         final List<Result> previous = getChunk(entry.getValue().getPreviousVersion(), minExecutionCount, entry.getValue().getPrevius());
         currentChunk.getResult().addAll(previous);

         final List<Result> current = getChunk(entry.getValue().getVersion(), minExecutionCount, entry.getValue().getCurrent());
         currentChunk.getResult().addAll(current);

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
               for (final Result r : entry.getValue().getPrevius()) {
                  LOG.debug("Value: {} Executions: {} Repetitions: {}", r.getValue(), r.getExecutionTimes(), r.getRepetitions());
               }
               for (final Result r : entry.getValue().getCurrent()) {
                  LOG.debug("Value:  {} Executions: {} Repetitions: {}", r.getValue(), r.getExecutionTimes(), r.getRepetitions());
               }
               LOG.debug("Too few correct measurements: {} ", measurementFile.getAbsolutePath());
               LOG.debug("Measurements: {} / {}", currentChunk.getResult().size(), entry.getValue().getPrevius().size() + entry.getValue().getCurrent().size());
            }
         } catch (final JAXBException e) {
            e.printStackTrace();
         }
      }
   }

   public boolean checkChunk(final Chunk currentChunk) {
      return currentChunk.getResult().size() > 2;
   }

   private static final long ceilDiv(final long x, final long y) {
      return -Math.floorDiv(-x, y);
   }

   private List<Result> getChunk(final String version, final long minExecutionCount, List<Result> previous) {
      final List<Result> previousClean = ConfidenceInterval.getWarmedUpData(previous);
//      final List<Result> previousClean = ConfidenceInterval.getWarmupData(previous);
      for (final Iterator<Result> it = previousClean.iterator(); it.hasNext();) {
         final Result result = it.next();
         final int resultSize = result.getFulldata().getValue().size();
         final long expectedSize = ceilDiv(minExecutionCount, 2);
         if (resultSize == expectedSize && !Double.isNaN(result.getValue())) {
            cleanResult(version, result);
         } else {
            LOG.debug("Wrong size: {} Expected: {}", resultSize, expectedSize);
            it.remove();
         }
      }
      return previousClean;
   }

   private void cleanResult(final String version, final Result result) {
      result.setVersion(new Versioninfo());
      result.getVersion().setGitversion(version);
      result.setWarmupExecutions(result.getFulldata().getValue().size());
      result.setExecutionTimes(result.getFulldata().getValue().size());
      result.setRepetitions(result.getRepetitions());
      result.setMin(null);
      result.setMax(null);
      result.setFirst10Percentile(null);
      result.setFulldata(new Fulldata());
   }
}
