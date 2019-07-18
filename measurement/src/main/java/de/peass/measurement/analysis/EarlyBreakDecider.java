package de.peass.measurement.analysis;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.testtransformation.JUnitTestTransformer;

public class EarlyBreakDecider {

   private static final double THRESHOLD_BREAK = 0.005;
   /**
    * When adjusting this value, it needs to be considered that measurement overhead and iterations multiplicate this value.
    */
   private static final int BIG_TESTCASE_THRESHOLD_MIKROSECONDS = 5000;

   private static final Logger LOG = LogManager.getLogger(EarlyBreakDecider.class);

   private final JUnitTestTransformer testTransformer;
   private final List<Double> before = new LinkedList<>();
   private final List<Double> after = new LinkedList<>();

   private double type1error = 0.01;
   private double type2error = 0.01;

   public double getType1error() {
      return type1error;
   }

   public void setType1error(double type1error) {
      this.type1error = type1error;
   }

   public double getType2error() {
      return type2error;
   }

   public void setType2error(double type2error) {
      this.type2error = type2error;
   }

   public EarlyBreakDecider(JUnitTestTransformer testTransformer, final File measurementFolder, final String version,
         final String versionOld, final TestCase testcase, final long currentChunkStart) throws JAXBException {
      this.testTransformer = testTransformer;
      loadData(measurementFolder, version, versionOld, testcase, currentChunkStart);
   }

   public boolean isBreakPossible(final int vmid) {
      boolean savelyDecidable = false;
      if (vmid > 3) {
         LOG.debug("T: {} {}", before, after);
         if ((before.size() > 3 && after.size() > 3)) {
            final double[] valsBefore = ArrayUtils.toPrimitive(before.toArray(new Double[0]));
            final double[] valsAfter = ArrayUtils.toPrimitive(after.toArray(new Double[0]));
            savelyDecidable = isSavelyDecidableBothHypothesis(vmid, valsBefore, valsAfter);
         } else if (vmid > 10) {
            LOG.debug("More than 10 executions and only {} / {} measurements - aborting", before.size(), after.size());
            return true;
         }
         // T statistic can not be determined if less than 2 values (produces exception..)

      }
      return savelyDecidable;
   }

   private void loadData(final File measurementFolder, final String version, final String versionOld, final TestCase testcase, final long currentChunkStart) throws JAXBException {
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

   public boolean isSavelyDecidableBothHypothesis(final int vmid, final double[] valsBefore, final double[] valsAfter) {
      boolean savelyDecidable = false;
      final DescriptiveStatistics statisticsBefore = new DescriptiveStatistics(valsBefore);
      final DescriptiveStatistics statisticsAfter = new DescriptiveStatistics(valsAfter);
      if (valsBefore.length > 30 && valsAfter.length > 30) {
         Relation relation = StatisticUtil.agnosticTTest(statisticsAfter, statisticsBefore, type1error, type2error);
         if (relation == Relation.EQUAL || relation == Relation.UNEQUAL) {
            LOG.info("Can savely decide: {}", relation);
            savelyDecidable = true;
         }
      }
      return savelyDecidable;
   }

}
