package de.dagere.peass.dependency.reader.twiceExecution;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.testtransformation.TestTransformer;

public class TwiceExecutableChecker {
   
   private static final Logger LOG = LogManager.getLogger(TwiceExecutableChecker.class);
   
   private final TestExecutor executor;
   private final TestTransformer transformer;

   private final ExecutionData executionData;

   public TwiceExecutableChecker(TestExecutor executor, ExecutionData executionData) {
      this.executor = executor;
      this.transformer = executor.getTestTransformer();
      this.executionData = executionData;
   }

   public void checkTwiceExecution(String commit, String predecessor, Set<TestMethodCall> tests) {
      transformer.getConfig().setIterations(2);
      executionData.addEmptyCommit(commit, predecessor);

      File twiceRunningLogFolder = new File(executor.getFolders().getTwiceRunningLogFolder(), commit);
      twiceRunningLogFolder.mkdir();
      executor.prepareKoPeMeExecution(new File(twiceRunningLogFolder, "twicePreparation.txt"));
      truncateLastRunResults();
      
      for (TestMethodCall testcase : tests) {
         executor.executeTest(testcase, twiceRunningLogFolder, transformer.getConfig().getExecutionConfig().getTimeout());

         checkTestExistance(commit, testcase);
      }
   }

   private void truncateLastRunResults() {
      try {
         FileUtils.deleteDirectory(executor.getFolders().getTempMeasurementFolder());
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   private void checkTestExistance(String commit, TestMethodCall testcase) {
      final Collection<File> folderCandidates = executor.getFolders().findTempClazzFolder(testcase);
      if (!folderCandidates.isEmpty()) {
         File folder = folderCandidates.iterator().next();
         final String methodname = testcase.getMethodWithParams();
         File oneResultFile = new File(folder, methodname + ".json");
         Kopemedata data = JSONDataLoader.loadData(oneResultFile);
         List<VMResult> firstDataCollectorContent = data.getFirstDatacollectorContent();
         if (firstDataCollectorContent.size() == 1 && firstDataCollectorContent.get(0).isError() == false) {
            LOG.info("Test is twice executable and therefore likely to be suitable for performance measurement: {}", testcase);
            executionData.addCall(commit, testcase);
         } else {
            LOG.info("Test is *not* twice executable and therefore likely to be *not* suitable for performance measurement: {}", testcase);
         }
      } else {
         LOG.info("Test is *not* twice executable and therefore likely to be *not* suitable for performance measurement: {}", testcase);
      }
   }
}
