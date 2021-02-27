package de.peass.measurement.analysis;

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
import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.analysis.data.TestCase;

public class ResultLoader {

   private static final Logger LOG = LogManager.getLogger(ResultLoader.class);

   private final MeasurementConfiguration config;
   final File measurementFolder;
   final TestCase testcase;
   final long currentChunkStart;

   private final List<Double> before = new LinkedList<>();
   private final List<Double> after = new LinkedList<>();

   public ResultLoader(MeasurementConfiguration config, final File measurementFolder, final TestCase testcase,
         final long currentChunkStart) {
      this.config = config;
      this.measurementFolder = measurementFolder;
      this.testcase = testcase;
      this.currentChunkStart = currentChunkStart;
   }

   public void loadData() throws JAXBException {
      final File kopemeFile = new File(measurementFolder, testcase.getShortClazz() + "_" + testcase.getMethod() + ".xml");
      final XMLDataLoader loader = new XMLDataLoader(kopemeFile);
      if (loader.getFullData().getTestcases().getTestcase().size() > 0) {
         final Datacollector dataCollector = loader.getFullData().getTestcases().getTestcase().get(0).getDatacollector().get(0);
         final Chunk realChunk = MultipleVMTestUtil.findChunk(currentChunkStart, dataCollector);
         LOG.debug("Chunk size: {}", realChunk.getResult().size());
         for (final Result result : realChunk.getResult()) {
            if (result.getIterations() + result.getWarmup() == config.getIterations() &&
                  result.getRepetitions() == config.getRepetitions()) {
               if (result.getVersion().getGitversion().equals(config.getVersionOld())) {
                  before.add(result.getValue());
               }
               if (result.getVersion().getGitversion().equals(config.getVersion())) {
                  after.add(result.getValue());
               }
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
}