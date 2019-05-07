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
            savelyDecidable = isSavelyDecidable2(vmid, valsBefore, valsAfter);
         } else if (vmid > 10) {
            LOG.debug("More than 10 executions and only {} / {} measurements - aborting", before.size(), after.size());
            return true;
         }
         // T statistic can not be determined if less than 2 values (produces exception..)

      }
      return savelyDecidable;
   }

   public void loadData(final File measurementFolder, final String version, final String versionOld, final TestCase testcase, final long currentChunkStart) throws JAXBException {
      final File kopemeFile = new File(measurementFolder, testcase.getShortClazz() + "_" + testcase.getMethod() + ".xml");
      final XMLDataLoader loader = new XMLDataLoader(kopemeFile);
      final Chunk realChunk = MultipleVMTestUtil.findChunk(currentChunkStart, loader.getFullData().getTestcases().getTestcase().get(0).getDatacollector().get(0));
      for (final Result result : realChunk.getResult()) {
         if (result.getExecutionTimes() == testTransformer.getIterations() && result.getRepetitions() == testTransformer.getRepetitions()) {
            if (result.getVersion().getGitversion().equals(versionOld)) {
               before.add(result.getValue());
            }
            if (result.getVersion().getGitversion().equals(version)) {
               after.add(result.getValue());
            }
         }
      }
   }

   public static boolean isSavelyDecidable2(final int vmid, final double[] valsBefore, final double[] valsAfter) {
      boolean savelyDecidable = false;
      final DescriptiveStatistics statisticsBefore = new DescriptiveStatistics(valsBefore);
      final DescriptiveStatistics statisticsAfter = new DescriptiveStatistics(valsAfter);
      final double tvalue = TestUtils.t(valsBefore, valsAfter);
      final double deviationBefore = statisticsBefore.getStandardDeviation() / statisticsBefore.getMean();
      final double deviationAfter = statisticsAfter.getStandardDeviation() / statisticsAfter.getMean();
      if (valsBefore.length > 10 && valsAfter.length > 10) {
//         savelyDecidable = testValueAddition(statisticsBefore, statisticsAfter);

         if (Math.abs(tvalue) > 5 || Math.abs(tvalue) < 0.1) {
            LOG.info("In VM iteration {}, t-value was {} - skipping rest of vm executions.", vmid, tvalue);
            savelyDecidable = true;
         }
         if (valsBefore.length > 40 && valsBefore.length > 40 && 
               Math.abs(tvalue) > 3 || Math.abs(tvalue) < 0.2) {
            LOG.info("In VM iteration {}, t-value was {} - skipping rest of vm executions.", vmid, tvalue);
            savelyDecidable = true;
         }
         
         LOG.info("In VM iteration {}, Standard-Deviations are {} {}", vmid, deviationBefore, deviationAfter);

         if (deviationBefore < THRESHOLD_BREAK && deviationAfter < THRESHOLD_BREAK) {
            savelyDecidable = true;
            LOG.info("Savely decidable by deviations");
         }

         if ((Math.abs(statisticsBefore.getMean()) > BIG_TESTCASE_THRESHOLD_MIKROSECONDS || Math.abs(statisticsAfter.getMean()) > BIG_TESTCASE_THRESHOLD_MIKROSECONDS)
               && (deviationBefore < 2 * THRESHOLD_BREAK && deviationAfter < 2 * THRESHOLD_BREAK)
               || (Math.abs(tvalue) > 5 || Math.abs(tvalue) < 0.1)) {
            savelyDecidable = true;
            LOG.info("Savely decidable by deviations - big testcase");
         }
      }
      return savelyDecidable;
   }

   private static boolean testValueAddition(final DescriptiveStatistics statisticsBefore, final DescriptiveStatistics statisticsAfter) {
      boolean savelyDecidable;
      if (statisticsBefore.getMean() < statisticsAfter.getMean()) {
         savelyDecidable = testValueAdditionordered(statisticsBefore, statisticsAfter);
      } else {
         savelyDecidable = testValueAdditionordered(statisticsAfter, statisticsBefore);
      }
      return savelyDecidable;
   }

   /**
    * Tests whether t-test maintains same relation if mean +/- standard deviation is added
    * 
    * @param smaller
    * @param bigger
    * @return
    */
   private static boolean testValueAdditionordered(final DescriptiveStatistics smaller, final DescriptiveStatistics bigger) {
      boolean savelyDecidable = false;
      final boolean isChange = TestUtils.tTest(smaller, bigger, 0.02);
      // System.out.println("Before: " + bigger.getMean() + " " + bigger.getStandardDeviation() / bigger.getMean() + " After: " + smaller.getMean() + " "
      // + smaller.getStandardDeviation() / smaller.getMean() + " " + isChange);
      for (int i = 0; i < 20; i++) {
         if (isChange) {
            bigger.addValue(bigger.getMean() - bigger.getStandardDeviation());
            smaller.addValue(smaller.getMean() + smaller.getStandardDeviation());
         } else {
            bigger.addValue(bigger.getMean() + bigger.getStandardDeviation());
            smaller.addValue(smaller.getMean() - smaller.getStandardDeviation());
         }
      }
      final boolean isChange2 = TestUtils.tTest(smaller, bigger, 0.02);
      if (isChange == isChange2) {
         savelyDecidable = true;
      }
      if (savelyDecidable) {
         LOG.info("Savely decidable by extrem values - if mean + standard deviation is measured in lower distribution 10 times, relation persists");
         LOG.debug("Before: " + bigger.getMean() + " " + bigger.getStandardDeviation() / bigger.getMean() + " After: " + smaller.getMean() + " "
               + smaller.getStandardDeviation() / smaller.getMean() + " " + isChange2);
      }
      return savelyDecidable;
   }
}
