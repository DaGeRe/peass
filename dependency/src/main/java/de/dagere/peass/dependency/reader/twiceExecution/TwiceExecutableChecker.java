package de.dagere.peass.dependency.reader.twiceExecution;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.dagere.kopeme.datastorage.JSONDataLoader;
import de.dagere.kopeme.kopemedata.Kopemedata;
import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.testtransformation.TestTransformer;

public class TwiceExecutableChecker {
   private final TestExecutor executor;
   private final TestTransformer transformer;

   private final Map<TestMethodCall, Boolean> twiceExecutionInfo = new LinkedHashMap<>();

   public TwiceExecutableChecker(TestExecutor executor) {
      this.executor = executor;
      this.transformer = executor.getTestTransformer();
   }

   public void checkTwiceExecution(Set<TestMethodCall> tests) {
      transformer.getConfig().setIterations(2);

      executor.prepareKoPeMeExecution(new File(executor.getFolders().getTwiceRunningLogFolder(), "twicePreparation.txt"));
      for (TestMethodCall testcase : tests) {
         executor.executeTest(testcase, executor.getFolders().getTwiceRunningLogFolder(), transformer.getConfig().getExecutionConfig().getTimeout());

         final Collection<File> folderCandidates = executor.getFolders().findTempClazzFolder(testcase);
         if (!folderCandidates.isEmpty()) {
            File folder = folderCandidates.iterator().next();
            final String methodname = testcase.getMethodWithParams();
            File oneResultFile = new File(folder, methodname + ".json");
            Kopemedata data = JSONDataLoader.loadData(oneResultFile);
            List<VMResult> firstDataCollectorContent = data.getFirstDatacollectorContent();
            if (firstDataCollectorContent.size() == 1 && firstDataCollectorContent.get(0).isError() == false) {
               twiceExecutionInfo.put(testcase, true);
            } else {
               twiceExecutionInfo.put(testcase, false);
            }
         } else {
            twiceExecutionInfo.put(testcase, false);
         }
      }
   }

   public Map<TestMethodCall, Boolean> getTestProperties() {
      return twiceExecutionInfo;
   }
}
