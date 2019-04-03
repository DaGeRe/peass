package de.peass.dependencyprocessors;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.analysis.MultipleVMTestUtil;
import de.peass.testtransformation.JUnitTestTransformer;

public class AdaptiveTester extends DependencyTester {

   private static final double THRESHOLD_BREAK = 0.005;
   private static final Logger LOG = LogManager.getLogger(AdaptiveTester.class);

   public AdaptiveTester(final PeASSFolders folders, final boolean runInitial, final JUnitTestTransformer testgenerator, final int vms)
         throws IOException {
      super(folders, runInitial, testgenerator, vms);
   }

   @Override
   public void evaluate(final String version, final String versionOld, final TestCase testcase) throws IOException, InterruptedException, JAXBException {
      LOG.info("Executing test " + testcase.getClazz() + " " + testcase.getMethod() + " in versions {} and {}", versionOld, version);

      final File logFolder = getLogFolder(version, testcase);

      currentChunkStart = System.currentTimeMillis();
      for (int vmid = 0; vmid < vms; vmid++) {
         runOneComparison(version, versionOld, logFolder, testcase, vmid);

         final boolean savelyDecidable = isBreakPossible(folders.getFullMeasurementFolder(), version, versionOld,
               testcase, vmid, currentChunkStart);

         if (savelyDecidable) {
            LOG.debug("Savely decidable - finishing testing");
            break;
         }

         final boolean shouldBreak = updateExecutions(version, versionOld, testcase, vmid);
         if (shouldBreak) {
            LOG.debug("Too less executions possible - finishing testing.");
            break;
         }
      }
   }

   boolean updateExecutions(final String version, final String versionOld, final TestCase testcase, final int vmid) throws JAXBException {
      boolean shouldBreak = false;
      final Result versionOldResult = getLastResult(versionOld, testcase, vmid);
      final Result versionNewResult = getLastResult(version, testcase, vmid);
      if (vmid < 10) {
         if (versionOldResult == null || versionNewResult == null) {
            final int lessIterations = testTransformer.getIterations() / 5;
            if (versionOldResult == null) {
               final String problemReason = "Measurement for " + versionOld + " is null";
               LOG.error(problemReason);
            } else if (versionNewResult == null) {
               final String problemReason = "Measurement for " + version + " is null";
               LOG.error(problemReason);
            } else {
               LOG.error("Both null!");
            }
            shouldBreak = reduceExecutions(shouldBreak, lessIterations);
         } else if (versionOldResult.getExecutionTimes() < testTransformer.getIterations()
               || versionNewResult.getExecutionTimes() < testTransformer.getIterations()) {
            final int lessIterations;
            final String problemReason = "Measurement executions: Old: " + versionOldResult.getExecutionTimes() + " New: " + versionNewResult.getExecutionTimes();
            LOG.error(problemReason);
            final int minOfExecuted = (int) Math.min(versionOldResult.getExecutionTimes(), versionNewResult.getExecutionTimes()) - 2;
            lessIterations = Math.min(minOfExecuted, testTransformer.getIterations() / 2);
            // final int lessIterations = testTransformer.getIterations() / 5;
            shouldBreak = reduceExecutions(shouldBreak, lessIterations);
         } else if ((versionOldResult.getValue() > 10E7 || versionOldResult.getValue() > 10E7) && testTransformer.getIterations() > 10) {
            shouldBreak = reduceExecutions(shouldBreak, testTransformer.getIterations() / 5);
         }
      }
      
      return shouldBreak;
   }

   boolean reduceExecutions(boolean shouldBreak, final int lessIterations) {
      if (lessIterations > 3) {
         LOG.info("Reducing iterations too: {}", lessIterations);
         testTransformer.setIterations(lessIterations);
         testTransformer.setWarmupExecutions(0);
      } else {
         if (testTransformer.getRepetitions() > 10) {
            testTransformer.setRepetitions(10);
         } else {
            shouldBreak = true;
         }
      }
      return shouldBreak;
   }

   private Result getLastResult(final String versionCurrent, final TestCase testcase, final int vmid) throws JAXBException {
      final File resultFile = getResultFile(testcase, vmid, versionCurrent);
      if (resultFile.exists()) {
         final Kopemedata data = new XMLDataLoader(resultFile).getFullData();
         final Result lastResult = data.getTestcases().getTestcase().get(0).getDatacollector().get(0).getResult().get(0);
         return lastResult;
      } else {
         LOG.debug("Resultfile {} does not exist", resultFile);
         return null;
      }
   }

   public boolean isBreakPossible(final File measurementFolder, final String version,
         final String versionOld, final TestCase testcase, final int vmid, final long currentChunkStart)
         throws JAXBException {

      boolean savelyDecidable = false;
      if (vmid > 3) {
         final File kopemeFile = new File(measurementFolder, testcase.getShortClazz() + "_" + testcase.getMethod() + ".xml");
         final XMLDataLoader loader = new XMLDataLoader(kopemeFile);
         final List<Double> before = new LinkedList<>();
         final List<Double> after = new LinkedList<>();
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

   public static boolean isSavelyDecidable(final int vmid, final double[] valsBefore, final double[] valsAfter) {
      boolean savelyDecidable = false;
      final DescriptiveStatistics statisticsBefore = new DescriptiveStatistics(valsBefore);
      final DescriptiveStatistics statisticsAfter = new DescriptiveStatistics(valsAfter);
      final double tvalue = TestUtils.t(valsBefore, valsAfter);
      final double deviationBefore = statisticsBefore.getStandardDeviation() / statisticsBefore.getMean();
      final double deviationAfter = statisticsAfter.getStandardDeviation() / statisticsAfter.getMean();
      final int correctExecutions = valsBefore.length;
      if (correctExecutions > 10) {
         if (Math.abs(tvalue) > 6 || Math.abs(tvalue) < 0.1) {
            LOG.info("In VM iteration {}, t-value was {} - skipping rest of vm executions.", vmid, tvalue);
            savelyDecidable = true;
         }
         LOG.info("In VM iteration {}, Standard-Deviations are {} {}", vmid, deviationBefore, deviationAfter);

         if (deviationBefore < THRESHOLD_BREAK && deviationAfter < THRESHOLD_BREAK) {
            savelyDecidable = true;
            LOG.info("Savely decidable by deviations");
         }
      }
      if (correctExecutions > 3) {
         if (deviationBefore < THRESHOLD_BREAK && deviationAfter < THRESHOLD_BREAK && Math.abs(tvalue) > 6) {
            savelyDecidable = true;
            LOG.info("Savely decidable by deviations and big T-Value: {}", tvalue);
         }
      }
      return savelyDecidable;
   }

   /**
    * When adjusting this value, it needs to be considered that measurement overhead and iterations multiplicate this value.
    */
   private static final int BIG_TESTCASE_THRESHOLD_MIKROSECONDS = 5000;

   public static boolean isSavelyDecidable2(final int vmid, final double[] valsBefore, final double[] valsAfter) {
      boolean savelyDecidable = false;
      final DescriptiveStatistics statisticsBefore = new DescriptiveStatistics(valsBefore);
      final DescriptiveStatistics statisticsAfter = new DescriptiveStatistics(valsAfter);
      final double tvalue = TestUtils.t(valsBefore, valsAfter);
      final double deviationBefore = statisticsBefore.getStandardDeviation() / statisticsBefore.getMean();
      final double deviationAfter = statisticsAfter.getStandardDeviation() / statisticsAfter.getMean();
      if (vmid > 10) {
         savelyDecidable = testValueAddition(statisticsBefore, statisticsAfter);

         if (Math.abs(tvalue) > 5 || Math.abs(tvalue) < 0.2) {
            LOG.info("In VM iteration {}, t-value was {} - skipping rest of vm executions.", vmid, tvalue);
            savelyDecidable = true;
         }
         LOG.info("In VM iteration {}, Standard-Deviations are {} {}", vmid, deviationBefore, deviationAfter);

         if (deviationBefore < THRESHOLD_BREAK && deviationAfter < THRESHOLD_BREAK) {
            savelyDecidable = true;
            LOG.info("Savely decidable by deviations");
         }

         if ((Math.abs(statisticsBefore.getMean()) > BIG_TESTCASE_THRESHOLD_MIKROSECONDS || Math.abs(statisticsAfter.getMean()) > BIG_TESTCASE_THRESHOLD_MIKROSECONDS)
               && (deviationBefore < 3 * THRESHOLD_BREAK && deviationAfter < 3 * THRESHOLD_BREAK)
               || (Math.abs(tvalue) > 4 || Math.abs(tvalue) < 0.2)) {
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
      for (int i = 0; i < 10; i++) {
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
