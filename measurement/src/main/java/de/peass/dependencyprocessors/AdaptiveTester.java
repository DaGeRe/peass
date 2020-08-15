package de.peass.dependencyprocessors;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.analysis.EarlyBreakDecider;
import de.peass.measurement.analysis.ResultLoader;
import de.peass.measurement.organize.FolderDeterminer;
import de.peass.testtransformation.JUnitTestTransformer;

public class AdaptiveTester extends DependencyTester {

   private static final Logger LOG = LogManager.getLogger(AdaptiveTester.class);

   private int finishedVMs = 0;

   public AdaptiveTester(final PeASSFolders folders, final JUnitTestTransformer testgenerator)
         throws IOException {
      super(folders, testgenerator);

   }

   public void evaluate(final TestCase testcase) throws IOException, InterruptedException, JAXBException {
      LOG.info("Executing test " + testcase.getClazz() + " " + testcase.getMethod() + " in versions {} and {}", configuration.getVersionOld(), configuration.getVersion());

      new FolderDeterminer(folders).testResultFolders(configuration.getVersion(), configuration.getVersionOld(), testcase);

      final File logFolder = getLogFolder(configuration.getVersion(), testcase);

      currentChunkStart = System.currentTimeMillis();
      for (finishedVMs = 0; finishedVMs < configuration.getVms(); finishedVMs++) {
         runOneComparison(logFolder, testcase, finishedVMs);

         final boolean savelyDecidable = checkIsDecidable(testcase, finishedVMs);

         if (savelyDecidable) {
            LOG.debug("Savely decidable - finishing testing");
            break;
         }

         final boolean shouldBreak = updateExecutions(testcase, finishedVMs);
         if (shouldBreak) {
            LOG.debug("Too less executions possible - finishing testing.");
            break;
         }
      }
   }

   public int getFinishedVMs() {
      return finishedVMs;
   }

   public boolean checkIsDecidable(final TestCase testcase, final int vmid) throws JAXBException {
      final boolean savelyDecidable;
      if (configuration.isEarlyStop()) {
         final ResultLoader loader = new ResultLoader(configuration, folders.getFullMeasurementFolder(), testcase, currentChunkStart);
         loader.loadData();
         System.out.println(loader.getStatisticsAfter());
         DescriptiveStatistics statisticsBefore = loader.getStatisticsBefore();
         DescriptiveStatistics statisticsAfter = loader.getStatisticsAfter();

         final EarlyBreakDecider decider = new EarlyBreakDecider(configuration, statisticsAfter, statisticsBefore);
         decider.setType1error(configuration.getType1error());
         decider.setType2error(configuration.getType2error());
         savelyDecidable = decider.isBreakPossible(vmid);
      } else {
         savelyDecidable = false;
      }
      return savelyDecidable;
   }

   boolean updateExecutions(final TestCase testcase, final int vmid) throws JAXBException {
      boolean shouldBreak = false;
      final Result versionOldResult = getLastResult(configuration.getVersionOld(), testcase, vmid);
      final Result versionNewResult = getLastResult(configuration.getVersion(), testcase, vmid);
      if (vmid < 40) {
         int reducedIterations = Math.min(shouldReduce(configuration.getVersionOld(), versionOldResult),
                                  shouldReduce(configuration.getVersion(), versionNewResult));
         if (reducedIterations != testTransformer.getConfig().getIterations()) {
            final int lessIterations = testTransformer.getConfig().getIterations() / 5;
            shouldBreak = reduceExecutions(shouldBreak, lessIterations);
         }
      }

      return shouldBreak;
   }

   private int shouldReduce(final String version, final Result result) {
      final int reducedIterations;
      if (result == null) {
         reducedIterations = testTransformer.getConfig().getIterations() / 2;
         LOG.error("Measurement for {} is null", version);
      } else if (result.getExecutionTimes() < testTransformer.getConfig().getIterations()) {
         LOG.error("Measurement executions: {}", result.getExecutionTimes());
         final int minOfExecuted = (int) result.getExecutionTimes() - 2;
         reducedIterations = Math.min(minOfExecuted, testTransformer.getConfig().getIterations() / 2);
      } else if (result.getValue() > 10E7 && testTransformer.getConfig().getIterations() > 10) {
         reducedIterations = testTransformer.getConfig().getIterations() / 2;
      } else {
         reducedIterations = testTransformer.getConfig().getIterations();
      }
      return reducedIterations;
   }

   boolean reduceExecutions(boolean shouldBreak, final int lessIterations) {
      if (lessIterations > 3) {
         LOG.info("Reducing iterations too: {}", lessIterations);
         testTransformer.getConfig().setIterations(lessIterations);
         testTransformer.getConfig().setWarmup(0);
      } else {
         if (testTransformer.getConfig().getRepetitions() > 5) {
            final int reducedRepetitions = testTransformer.getConfig().getRepetitions() / 5;
            LOG.debug("Reducing repetitions to " + reducedRepetitions);
            testTransformer.getConfig().setRepetitions(reducedRepetitions);
         } else {
            shouldBreak = true;
         }
      }
      return shouldBreak;
   }

   public Result getLastResult(final String version, final TestCase testcase, final int vmid) throws JAXBException {
      final File resultFile = currentOrganizer.getResultFile(testcase, vmid, version);
      if (resultFile.exists()) {
         final Kopemedata data = new XMLDataLoader(resultFile).getFullData();
         final Result lastResult = data.getTestcases().getTestcase().get(0).getDatacollector().get(0).getResult().get(0);
         return lastResult;
      } else {
         LOG.debug("Resultfile {} does not exist", resultFile);
         return null;
      }
   }

}
