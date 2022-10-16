package de.dagere.peass.measurement.dependencyprocessors.reductioninfos;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.measurement.organize.ResultOrganizer;
import de.dagere.peass.utils.Constants;

public class ReductionManager {

   private static final Logger LOG = LogManager.getLogger(ReductionManager.class);

   private final MeasurementConfig measurementConfig;

   private final ReductionInformation reductionInformation = new ReductionInformation();

   public ReductionManager(MeasurementConfig configuration) {
      this.measurementConfig = configuration;
   }

   public boolean updateExecutions(final TestMethodCall testcase, final int vmid, ResultOrganizer organizer) {
      boolean shouldBreak = false;
      final VMResult commitOldResult = getLastResult(measurementConfig.getFixedCommitConfig().getCommitOld(), testcase, vmid, organizer);
      final VMResult commitCurrentResult = getLastResult(measurementConfig.getFixedCommitConfig().getCommit(), testcase, vmid, organizer);
      if (vmid < 40) {
         VMReductionInfo reductionOld = shouldReduce(measurementConfig.getFixedCommitConfig().getCommitOld(), commitOldResult);
         VMReductionInfo reductionCurrent = shouldReduce(measurementConfig.getFixedCommitConfig().getCommit(), commitCurrentResult);
         int reducedIterations = Math.min(reductionOld.getReductionToIterationCount(), reductionCurrent.getReductionToIterationCount());
         if (reducedIterations != measurementConfig.getIterations()) {
            reductionInformation.addReduction(vmid, reductionOld, reductionCurrent);

            LOG.error("Should originally run {} iterations, but did not succeed - reducing to {}", measurementConfig.getIterations(), reducedIterations);
            // final int lessIterations = testTransformer.getConfig().getIterations() / 5;
            shouldBreak = reduceExecutions(shouldBreak, reducedIterations);
            
            try {
               File reductionFile = organizer.getFolders().getReductionFile(testcase);
               Constants.OBJECTMAPPER.writeValue(reductionFile, commitCurrentResult);
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }

      return shouldBreak;
   }

   private VMReductionInfo shouldReduce(final String commit, final VMResult result) {
      final VMReductionInfo reductionInfo;
      if (result == null) {
         reductionInfo = new VMReductionInfo(measurementConfig.getIterations() / 2, ReductionReasons.NO_RESULT_FILE);
         LOG.error("Measurement for {} is null", commit);
      } else if (result.getIterations() < measurementConfig.getIterations()) {
         LOG.error("Measurement executions: {}", result.getIterations());
         final int minOfExecuted = (int) result.getIterations() - 2;
         reductionInfo = new VMReductionInfo(minOfExecuted, ReductionReasons.TOO_LESS_ITERATIONS);
      } else if (Double.isNaN(result.getValue())) {
         LOG.error("Measurement executions: {}", result.getIterations());
         reductionInfo = new VMReductionInfo(measurementConfig.getIterations() / 2, ReductionReasons.NO_DATA);
      } else {
         reductionInfo = new VMReductionInfo(measurementConfig.getIterations());
      }
      return reductionInfo;
   }

   public boolean reduceExecutions(boolean shouldBreak, final int lessIterations) {
      if (lessIterations > 3) {
         LOG.info("Reducing iterations too: {}", lessIterations);
         measurementConfig.setIterations(lessIterations);
         measurementConfig.setWarmup(0);
      } else {
         if (measurementConfig.getRepetitions() > 5) {
            final int reducedRepetitions = measurementConfig.getRepetitions() / 5;
            LOG.debug("Reducing repetitions to " + reducedRepetitions);
            measurementConfig.setRepetitions(reducedRepetitions);
         } else {
            LOG.error("Cannot reduce iterations ({}) or repetitions ({}) anymore", measurementConfig.getIterations(), measurementConfig.getRepetitions());
            shouldBreak = true;
         }
      }
      return shouldBreak;
   }

   public VMResult getLastResult(final String version, final TestMethodCall testcase, final int vmid, ResultOrganizer organizer) {
      System.out.println("Getting " + version + " " + testcase + " " + vmid);
      final File resultFile = organizer.getResultFile(testcase, vmid, version);
      if (resultFile.exists()) {
         final Kopemedata data = JSONDataLoader.loadData(resultFile);
         final VMResult lastResult = data.getFirstResult();
         return lastResult;
      } else {
         LOG.debug("Resultfile {} does not exist", resultFile);
         return null;
      }
   }
}
