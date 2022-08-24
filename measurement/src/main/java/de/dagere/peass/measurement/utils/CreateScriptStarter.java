package de.dagere.peass.measurement.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.analysis.measurement.ProjectStatistics;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.parameters.ExecutionConfigMixin;
import de.dagere.peass.config.parameters.KiekerConfigMixin;
import de.dagere.peass.config.parameters.MeasurementConfigurationMixin;
import de.dagere.peass.config.parameters.StatisticsConfigMixin;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.testData.TestClazzCall;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;
import de.dagere.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * Creates a script for running a set of tests based on a staticSelectionFile (and optionally an executionfile) in order to start test executions.
 * 
 * @author reichelt
 *
 */
@Command(description = "Creates a script (bash or slurm) to run a set of tests", name = "createScript")
public class CreateScriptStarter implements Callable<Void> {

   @Option(names = { "-experimentId", "--experimentId" }, description = "Id of the experiment")
   protected String experimentId = "default";

   @Option(names = { "-staticSelectionFile", "--staticSelectionFile" }, description = "Path to the static test selection file")
   protected File staticSelectionFile;

   @Option(names = { "-executionFile", "--executionFile" }, description = "Path to the executionfile")
   protected File executionFile;

   @Option(names = { "-changeFile", "--changeFile" }, description = "Path to the change file")
   protected File[] changeFile;

   @Option(names = { "-statisticsFile", "--statisticsFile" }, description = "Path to the statistics file")
   protected File[] statisticsFile;

   @Option(names = { "-alreadyFinishedFolder", "--alreadyFinishedFolder" }, description = "Path to folder where finished results are (each should be named treeMeasurementResults)")
   protected File[] alreadyFinishedFolder;

   @Option(names = { "-useSlurm", "--useSlurm" }, description = "Use slurm (if not specified, a bash script is created)")
   protected Boolean useSlurm = false;

   @Mixin
   MeasurementConfigurationMixin measurementConfigMixin;

   @Mixin
   ExecutionConfigMixin executionConfigMixin;

   private StaticTestSelection staticTestSelection;
   private ExecutionData executionData;

   private Map<String, List<String>> alreadyAnalyzed = new HashMap<>();

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final CreateScriptStarter command = new CreateScriptStarter();
      final CommandLine commandLine = new CommandLine(command);
      commandLine.execute(args);
   }

   @Override
   public Void call() throws Exception {
      if (staticSelectionFile != null) {
         staticTestSelection = Constants.OBJECTMAPPER.readValue(staticSelectionFile, StaticTestSelection.class);
      }
      if (executionFile != null) {
         executionData = Constants.OBJECTMAPPER.readValue(executionFile, ExecutionData.class);
         staticTestSelection = new StaticTestSelection(executionData);
      }
      if (executionData == null && staticTestSelection == null) {
         throw new RuntimeException("Dependencyfile and executionfile not readable - one needs to be defined!");
      }

      MeasurementConfig config = new MeasurementConfig(measurementConfigMixin, executionConfigMixin, new StatisticsConfigMixin(), new KiekerConfigMixin());

      PrintStream destination = System.out;

      if (alreadyFinishedFolder != null) {
         determineMeasuredTestCommitPairs();
      }

      if (changeFile == null) {
         generateMeasurementExecuteCommands(config, destination);
      } else {
         generateRCAExecuteCommands(config, destination);
      }

      return null;
   }

   private void determineMeasuredTestCommitPairs() throws IOException, StreamReadException, DatabindException {
      for (File folder : alreadyFinishedFolder) {
         for (File commitFolder : folder.listFiles()) {
            for (File testclazzFolder : commitFolder.listFiles()) {
               for (File resultFile : testclazzFolder.listFiles()) {
                  if (!resultFile.isDirectory()) {
                     CauseSearchData csd = Constants.OBJECTMAPPER.readValue(resultFile, CauseSearchData.class);
                     String commit = csd.getMeasurementConfig().getFixedCommitConfig().getCommit();
                     // System.out.println(csd.getTestcase() + " " + commit);

                     List<String> finishedTests = alreadyAnalyzed.get(commit);
                     if (finishedTests == null) {
                        finishedTests = new LinkedList<>();
                        alreadyAnalyzed.put(commit, finishedTests);
                     }
                     finishedTests.add(csd.getTestcase());
                  }
               }
            }
         }
      }
   }

   private void generateMeasurementExecuteCommands(MeasurementConfig config, PrintStream destination) throws IOException, StreamReadException, DatabindException {
      RunCommandWriter writer;
      if (useSlurm) {
         destination.println("timestamp=$(date +%s)");
         writer = new RunCommandWriterSlurm(config, System.out, experimentId, staticTestSelection);
      } else {
         writer = new RunCommandWriter(config, destination, experimentId, staticTestSelection);
      }

      if (statisticsFile != null) {
         for (File statisticFile : statisticsFile) {
            ProjectStatistics statistics = Constants.OBJECTMAPPER.readValue(statisticFile, ProjectStatistics.class);
            for (Entry<String, Map<TestMethodCall, TestcaseStatistic>> commitEntry : statistics.getStatistics().entrySet()) {
               String commit = commitEntry.getKey();
               for (Entry<TestMethodCall, TestcaseStatistic> methodCallEntry : commitEntry.getValue().entrySet()) {
                  TestMethodCall methodCall = methodCallEntry.getKey();
                  TestSet currentCommitTestSet = executionData.getCommits().get(commit);
                  currentCommitTestSet.removeTest(methodCall.onlyClazz(), methodCall.getMethod());
               }
            }
               
         }
      }

      generateExecuteCommands(staticTestSelection, executionData, experimentId, writer);
   }

   private void generateRCAExecuteCommands(MeasurementConfig config, PrintStream destination) throws IOException, StreamReadException, DatabindException {
      RunCommandWriter writer;
      ExecutionData mergedExecutions = mergeChangeExecutions();

      writer = new RunCommandWriterRCA(config, destination, experimentId, mergedExecutions);
      generateExecuteCommands(staticTestSelection, mergedExecutions, experimentId, writer);
   }

   private ExecutionData mergeChangeExecutions() throws IOException, StreamReadException, DatabindException {
      ExecutionData mergedExecutions = new ExecutionData();
      mergedExecutions.setUrl(executionData.getUrl());
      for (Entry<String, TestSet> commit : executionData.getCommits().entrySet()) {
         mergedExecutions.addEmptyCommit(commit.getKey(), commit.getValue().getPredecessor());
      }
      for (File changeFileEntry : changeFile) {
         ProjectChanges changes = Constants.OBJECTMAPPER.readValue(changeFileEntry, ProjectChanges.class);
         for (Map.Entry<String, Changes> changeEntry : changes.getCommitChanges().entrySet()) {
            String commit = changeEntry.getKey();
            TestSet tests = changeEntry.getValue().getTests();
            mergedExecutions.addCall(commit, tests);
         }
      }
      return mergedExecutions;
   }

   public void generateExecuteCommands(final StaticTestSelection dependencies, final ExecutionData changedTests, final String experimentId, final PrintStream goal)
         throws IOException {
      generateExecuteCommands(dependencies, changedTests, experimentId, new RunCommandWriterSlurm(new MeasurementConfig(30), goal, experimentId, dependencies));
   }

   public static void generateExecuteCommands(final ExecutionData changedTests, final String experimentId, final PrintStream goal) throws IOException {
      generateExecuteCommands(changedTests, experimentId, new RunCommandWriterSlurm(new MeasurementConfig(30), goal, experimentId, changedTests));
   }

   public static void generateExecuteCommands(final ExecutionData changedTests, final String experimentId, final RunCommandWriter writer)
         throws IOException {
      int i = 0;
      for (Map.Entry<String, TestSet> entry : changedTests.getCommits().entrySet()) {
         for (final Entry<TestClazzCall, Set<String>> testcase : entry.getValue().getTestcases().entrySet()) {
            for (final String method : testcase.getValue()) {
               final String testcaseName = testcase.getKey().getClazz() + "#" + method;
               writer.createSingleMethodCommand(i, entry.getKey(), testcaseName);

            }
         }
         i++;
      }
   }

   public void generateExecuteCommands(final StaticTestSelection dependencies, final ExecutionData changedTests, final String experimentId, final RunCommandWriter writer)
         throws IOException {
      final String[] commits = dependencies.getCommitNames();
      for (int commitIndex = 0; commitIndex < commits.length; commitIndex++) {
         final String endcommit = commits[commitIndex];
         // System.out.println("-startversion " + startversion + " -endversion " + endversion);
         if (changedTests == null) {
            final Set<TestMethodCall> tests = dependencies.getCommits().get(endcommit).getTests().getTestMethods();
            writer.createFullVersionCommand(commitIndex, endcommit, tests);
         } else if (changedTests != null && changedTests.getCommits().containsKey(endcommit)) {
            for (final Entry<TestClazzCall, Set<String>> testcase : changedTests.getCommits().get(endcommit).getTestcases().entrySet()) {
               for (final String method : testcase.getValue()) {
                  final String testcaseName = testcase.getKey().getClazz() + "#" + method;
                  List<String> alreadyAnalyzedTests = alreadyAnalyzed.get(endcommit);
                  boolean analyzed = false;
                  if (alreadyAnalyzedTests != null && alreadyAnalyzedTests.contains(testcaseName)) {
                     analyzed = true;
                  }
                  if (!analyzed) {
                     writer.createSingleMethodCommand(commitIndex, endcommit, testcaseName);
                  }
               }
            }
         }
      }
   }
}
