package de.peass.dependencyprocessors;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.analysis.EarlyBreakDecider;
import de.peass.testtransformation.JUnitTestTransformer;

public class AdaptiveTester extends DependencyTester {

   private static final Logger LOG = LogManager.getLogger(AdaptiveTester.class);

   public AdaptiveTester(final PeASSFolders folders, final JUnitTestTransformer testgenerator, final int vms)
         throws IOException {
      super(folders, testgenerator, vms);
   }

   @Override
   public void evaluate(final String version, final String versionOld, final TestCase testcase) throws IOException, InterruptedException, JAXBException {
      LOG.info("Executing test " + testcase.getClazz() + " " + testcase.getMethod() + " in versions {} and {}", versionOld, version);

      final File logFolder = getLogFolder(version, testcase);

      currentChunkStart = System.currentTimeMillis();
      for (int vmid = 0; vmid < vms; vmid++) {
         runOneComparison(version, versionOld, logFolder, testcase, vmid);

         EarlyBreakDecider decider = new EarlyBreakDecider(testTransformer, folders.getFullMeasurementFolder(), version, versionOld,
               testcase, currentChunkStart);
         final boolean savelyDecidable = decider.isBreakPossible(vmid);

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
      final File resultFile = currentOrganizer.getResultFile(testcase, vmid, versionCurrent);
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
