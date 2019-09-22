package de.peass.measurement.analysis;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.testtransformation.JUnitTestTransformer;

public class EarlyBreakDecider {

   private static final Logger LOG = LogManager.getLogger(EarlyBreakDecider.class);

   private final JUnitTestTransformer testTransformer;
   private final StatisticalSummary statisticsBefore;
   private final StatisticalSummary statisticsAfter;

   private double type1error = 0.01;
   private double type2error = 0.01;

   public EarlyBreakDecider(final JUnitTestTransformer testTransformer, final File measurementFolder, final String version,
         final String versionOld, final TestCase testcase, final long currentChunkStart) throws JAXBException {
      this.testTransformer = testTransformer;
      final ResultLoader loader = new ResultLoader(measurementFolder, version, versionOld, testcase, currentChunkStart);
      loader.loadData();
      final double[] valsBefore = loader.getValsBefore();
      final double[] valsAfter = loader.getValsAfter();
      statisticsBefore = new DescriptiveStatistics(valsBefore);
      statisticsAfter = new DescriptiveStatistics(valsAfter);
   }

   public EarlyBreakDecider(final JUnitTestTransformer testTransformer, final StatisticalSummary statisticsOld, final StatisticalSummary statistics) throws JAXBException {
      this.testTransformer = testTransformer;
      statisticsBefore = statisticsOld;
      statisticsAfter = statistics;
   }

   public boolean isBreakPossible(final int vmid) {
      boolean savelyDecidable = false;
      if (vmid > 3) {
         LOG.debug("T: {} {}", statisticsBefore.getN(), statisticsAfter.getN());
         if ((statisticsBefore.getN() > 3 && statisticsAfter.getN() > 3)) {
            savelyDecidable = isSavelyDecidableBothHypothesis(vmid);
         } else if (vmid > 10) {
            LOG.debug("More than 10 executions and only {} / {} measurements - aborting", statisticsBefore.getN(), statisticsAfter.getN());
            return true;
         }
         // T statistic can not be determined if less than 2 values (produces exception..)
      }
      return savelyDecidable;
   }

   public boolean isSavelyDecidableBothHypothesis(final int vmid) {
      boolean savelyDecidable = false;
      if (statisticsBefore.getN() > 30 && statisticsAfter.getN() > 30) {
         final Relation relation = StatisticUtil.agnosticTTest(statisticsBefore, statisticsAfter, type1error, type2error);
         if (relation == Relation.EQUAL || relation == Relation.UNEQUAL) {
            LOG.info("Can savely decide: {}", relation);
            savelyDecidable = true;
         }
      }
      return savelyDecidable;
   }

   public double getType1error() {
      return type1error;
   }

   public void setType1error(final double type1error) {
      this.type1error = type1error;
   }

   public double getType2error() {
      return type2error;
   }

   public void setType2error(final double type2error) {
      this.type2error = type2error;
   }

   private class ResultLoader {
      final File measurementFolder;
      final String version;
      final String versionOld;
      final TestCase testcase;
      final long currentChunkStart;

      private final List<Double> before = new LinkedList<>();
      private final List<Double> after = new LinkedList<>();

      public ResultLoader(final File measurementFolder, final String version, final String versionOld, final TestCase testcase, final long currentChunkStart) {
         this.measurementFolder = measurementFolder;
         this.version = version;
         this.versionOld = versionOld;
         this.testcase = testcase;
         this.currentChunkStart = currentChunkStart;
      }

      void loadData() throws JAXBException {
         final File kopemeFile = new File(measurementFolder, testcase.getShortClazz() + "_" + testcase.getMethod() + ".xml");
         final XMLDataLoader loader = new XMLDataLoader(kopemeFile);
         if (loader.getFullData().getTestcases().getTestcase().size() > 0) {
            final Datacollector dataCollector = loader.getFullData().getTestcases().getTestcase().get(0).getDatacollector().get(0);
            final Chunk realChunk = MultipleVMTestUtil.findChunk(currentChunkStart, dataCollector);
            LOG.debug("Chunk size: {}", realChunk.getResult().size());
            for (final Result result : realChunk.getResult()) {
               if (result.getExecutionTimes() + result.getWarmupExecutions() == testTransformer.getIterations() && result.getRepetitions() == testTransformer.getRepetitions()) {
                  if (result.getVersion().getGitversion().equals(versionOld)) {
                     before.add(result.getValue());
                  }
                  if (result.getVersion().getGitversion().equals(version)) {
                     after.add(result.getValue());
                  }
               }
            }
         }
      }

      double[] getValsBefore() {
         return ArrayUtils.toPrimitive(before.toArray(new Double[0]));
      }

      double[] getValsAfter() {
         return ArrayUtils.toPrimitive(after.toArray(new Double[0]));
      }
   }

}
