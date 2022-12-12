package de.dagere.peass.dependency.reader;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.javaparser.ParseException;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.TestSelectionConfig;
import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.DependencyManager;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.persistence.CommitStaticSelection;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependency.reader.twiceExecution.TwiceExecutableChecker;
import de.dagere.peass.dependency.traces.TraceFileMapping;
import de.dagere.peass.dependency.traces.coverage.CoverageSelectionExecutor;
import de.dagere.peass.dependency.traces.coverage.CoverageSelectionInfo;
import de.dagere.peass.dependency.traces.diff.DiffFileGenerator;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.CommitIterator;
import de.dagere.peass.vcs.GitCommitWriter;

/**
 * Shared functions for dependency reading, which are both used if dependencies are read fully or if one continues a dependency reading process.
 * 
 * @author reichelt
 *
 */
public class DependencyReader {

   private static final Logger LOG = LogManager.getLogger(DependencyReader.class);

   private final TestSelectionConfig testSelectionConfig;
   protected final StaticTestSelection staticSelectionResult = new StaticTestSelection();
   private final ExecutionData executionResult = new ExecutionData();
   private final ExecutionData coverageBasedSelection = new ExecutionData();
   private final CoverageSelectionInfo coverageSelectionInfo = new CoverageSelectionInfo();
   private final ExecutionData twiceExecutableSelected = new ExecutionData();

   private final CoverageSelectionExecutor coverageExecutor;
   private final TwiceExecutableChecker twiceExecutableChecker;

   protected final ResultsFolders resultsFolders;

   protected final PeassFolders folders;
   protected CommitIterator iterator;
   protected String lastRunningVersion;
   private final CommitKeeper skippedNoChange;

   private final KiekerConfig kiekerConfig;
   private final ExecutionConfig executionConfig;
   private final EnvironmentVariables env;

   protected final DependencyManager dependencyManager;
   private ChangeManager changeManager;
   private StaticChangeHandler staticChangeHandler;

   private final DependencySizeRecorder sizeRecorder = new DependencySizeRecorder();
   private final TraceFileMapping traceFileMapping = new TraceFileMapping();

   public DependencyReader(final TestSelectionConfig dependencyConfig, final PeassFolders folders,
         final ResultsFolders resultsFolders, final String url, final CommitIterator iterator,
         final ChangeManager changeManager, final ExecutionConfig executionConfig, final KiekerConfig kiekerConfig, final EnvironmentVariables env) {
      this.testSelectionConfig = dependencyConfig;
      this.resultsFolders = resultsFolders;
      this.iterator = iterator;
      this.folders = folders;
      this.skippedNoChange = new CommitKeeper(new File("/dev/null"));
      this.executionConfig = executionConfig;
      this.kiekerConfig = kiekerConfig;
      this.env = env;

      setURLs(url);

      dependencyManager = new DependencyManager(folders, executionConfig, kiekerConfig, env);
      coverageExecutor = new CoverageSelectionExecutor(traceFileMapping, coverageBasedSelection, coverageSelectionInfo);
      twiceExecutableChecker = new TwiceExecutableChecker(getExecutor(), twiceExecutableSelected);

      this.changeManager = changeManager;

      if (!kiekerConfig.isUseKieker()) {
         throw new RuntimeException("Dependencies may only be read if Kieker is enabled!");
      }
   }

   private void setURLs(final String url) {
      staticSelectionResult.setUrl(url);
      executionResult.setUrl(url);
      coverageBasedSelection.setUrl(url);
      twiceExecutableSelected.setUrl(url);
   }

   /**
    * Starts reading dependencies
    * 
    * @param projectFolder
    * @param dependencyFile
    * @param url
    * @param iterator
    */
   public DependencyReader(final TestSelectionConfig dependencyConfig, final PeassFolders folders, final ResultsFolders resultsFolders, final String url,
         final CommitIterator iterator,
         final CommitKeeper skippedNoChange, final ExecutionConfig executionConfig, final KiekerConfig kiekerConfig, final EnvironmentVariables env) {
      this.testSelectionConfig = dependencyConfig;
      this.resultsFolders = resultsFolders;
      this.iterator = iterator;
      this.folders = folders;
      this.skippedNoChange = skippedNoChange;
      this.executionConfig = executionConfig;
      this.kiekerConfig = kiekerConfig;
      this.env = env;

      setURLs(url);

      dependencyManager = new DependencyManager(folders, executionConfig, kiekerConfig, env);

      coverageExecutor = new CoverageSelectionExecutor(traceFileMapping, coverageBasedSelection, coverageSelectionInfo);
      twiceExecutableChecker = new TwiceExecutableChecker(getExecutor(), twiceExecutableSelected);

      if (!kiekerConfig.isUseKieker()) {
         throw new RuntimeException("Dependencies may only be read if Kieker is enabled!");
      }
   }

   /**
    * Reads the dependencies of the tests
    */
   public boolean readDependencies() {
      try {
         LOG.debug("Analysing {} entries", iterator.getRemainingSize());
         sizeRecorder.setPrunedSize(dependencyManager.getDependencyMap().size());

         changeManager.saveOldClasses();
         lastRunningVersion = iterator.getCommitName();
         while (iterator.hasNextCommit()) {
            iterator.goToNextCommit();
            readCommit();
         }

         LOG.debug("Finished dependency-reading");
         return true;
      } catch (IOException | InterruptedException | ParseException e) {
         e.printStackTrace();
         return false;
      }
   }

   public void readCommit() throws IOException, InterruptedException, ParseException {
      final int tests = analyseCommit(changeManager);
      GitCommitWriter.writeCurrentCommits(folders, iterator.getCommits(), resultsFolders);
      DependencyReaderUtil.write(staticSelectionResult, resultsFolders.getStaticTestSelectionFile());
      if (testSelectionConfig.isGenerateTraces()) {
         Constants.OBJECTMAPPER.writeValue(resultsFolders.getTraceTestSelectionFile(), executionResult);
         if (testSelectionConfig.isGenerateCoverageSelection()) {
            Constants.OBJECTMAPPER.writeValue(resultsFolders.getCoverageSelectionFile(), coverageBasedSelection);
            Constants.OBJECTMAPPER.writeValue(resultsFolders.getCoverageInfoFile(), coverageSelectionInfo);
         }
         if (testSelectionConfig.isGenerateTwiceExecutability()) {
            Constants.OBJECTMAPPER.writeValue(resultsFolders.getTwiceExecutableFile(), twiceExecutableSelected);
         }
      }

      sizeRecorder.addVersionSize(dependencyManager.getDependencyMap().size(), tests);

      dependencyManager.getExecutor().deleteTemporaryFiles();
      TooBigLogCleaner.cleanJSONFolder(folders);
      TooBigLogCleaner.cleanTooBigLogs(folders, iterator.getCommitName());
   }

   /**
    * Determines the tests that may have got new dependencies, writes that changes (i.e. the tests that need to be run in that version) and re-runs the tests in order to get the
    * updated test dependencies.
    * 
    * @param dependencyFile
    * @param dependencyManager
    * @param dependencies
    * @param staticSelectionResult
    * @param commit
    * @return
    * @throws IOException
    * @throws InterruptedException
    * @throws ParseException
    */
   public int analyseCommit(final ChangeManager changeManager) throws IOException, ParseException {
      final String commit = iterator.getCommitName();
      if (!testSelectionConfig.isSkipProcessSuccessRuns()) {
         if (!dependencyManager.getExecutor().isCommitRunning(iterator.getCommitName())) {
            documentFailure(commit);
            return 0;
         }
      }

      dependencyManager.getExecutor().loadClasses();

      final DependencyReadingInput input = new DependencyReadingInput(changeManager.getChanges(lastRunningVersion), lastRunningVersion);
      changeManager.saveOldClasses();
      lastRunningVersion = iterator.getCommitName();

      if (executionConfig.isCreateDetailDebugFiles()) {
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "initialdependencies_" + commit + ".json"), dependencyManager.getDependencyMap());
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "changes_" + commit + ".json"), input.getChanges());
      }

      if (input.getChanges().size() > 0) {
         return analyseChanges(commit, input);
      } else {
         addEmptyCommitData(commit, input);
         return 0;
      }
   }

   private void addEmptyCommitData(final String commit, final DependencyReadingInput input) {
      CommitStaticSelection emptyCommit = new CommitStaticSelection();
      emptyCommit.setJdk(dependencyManager.getExecutor().getJDKVersion());
      emptyCommit.setRunning(true);
      emptyCommit.setPredecessor(input.getPredecessor());
      staticSelectionResult.getCommits().put(commit, emptyCommit);
      if (testSelectionConfig.isGenerateTraces()) {
         executionResult.addEmptyCommit(commit, null);
         coverageBasedSelection.addEmptyCommit(commit, null);
      }
      if (testSelectionConfig.isGenerateTwiceExecutability()) {
         twiceExecutableSelected.addEmptyCommit(commit, null);
      }
      skippedNoChange.addCommit(commit, "No Change at all");
   }

   private int analyseChanges(final String commit, final DependencyReadingInput input)
         throws IOException, ParseException {
      final CommitStaticSelection newCommitInfo = staticChangeHandler.handleStaticAnalysisChanges(commit, input, dependencyManager.getModuleClassMapping());

      if (!testSelectionConfig.isDoNotUpdateDependencies()) {
         TraceChangeHandler traceChangeHandler = new TraceChangeHandler(dependencyManager, folders, executionConfig, commit);
         traceChangeHandler.handleTraceAnalysisChanges(newCommitInfo);

         if (testSelectionConfig.isGenerateTraces()) {
            generateTraces(commit, newCommitInfo);

            deriveAdditionalTestProperties(commit, newCommitInfo);
         }
      } else {
         LOG.debug("Not updating dependencies since doNotUpdateDependencies was set - only returning dependencies based on changed classes");
      }
      staticSelectionResult.getCommits().put(commit, newCommitInfo);

      final int changedClazzCount = calculateChangedClassCount(newCommitInfo);
      return changedClazzCount;
   }

   private void deriveAdditionalTestProperties(final String commit, final CommitStaticSelection newCommitInfo) throws IOException {
      TestSet dynamicallySelected = executionResult.getCommits().get(commit);
      if (testSelectionConfig.isGenerateTwiceExecutability()) {
         twiceExecutableChecker.checkTwiceExecution(commit, newCommitInfo.getPredecessor(), dynamicallySelected.getTestMethods());
         dynamicallySelected = twiceExecutableSelected.getCommits().get(commit);
         LOG.info("Left after twice execution checking: {}", dynamicallySelected);
      }

      if (testSelectionConfig.isGenerateCoverageSelection()) {
         coverageExecutor.generateCoverageBasedSelection(commit, newCommitInfo, dynamicallySelected);
      }
   }

   private void generateTraces(final String commit, final CommitStaticSelection newCommitInfo) throws IOException, ParseException {
      executionResult.addEmptyCommit(commit, newCommitInfo.getPredecessor());
      coverageBasedSelection.addEmptyCommit(commit, newCommitInfo.getPredecessor());
      TraceViewGenerator traceViewGenerator = new TraceViewGenerator(dependencyManager, folders, commit, traceFileMapping, kiekerConfig, testSelectionConfig);
      traceViewGenerator.generateViews(resultsFolders, newCommitInfo.getTests());

      DiffFileGenerator diffGenerator = new DiffFileGenerator(resultsFolders.getCommitDiffFolder(commit));
      diffGenerator.generateAllDiffs(commit, newCommitInfo, traceFileMapping, executionResult);
   }

   private int calculateChangedClassCount(final CommitStaticSelection newCommitInfo) {
      final int changedClazzCount = newCommitInfo.getChangedClazzes().values().stream().mapToInt(value -> {
         return value.getTestcases().values().stream().mapToInt(list -> list.size()).sum();
      }).sum();
      return changedClazzCount;
   }

   public void documentFailure(final String commit) {
      if (dependencyManager.getExecutor().isAndroid()) {
         staticSelectionResult.setAndroid(true);
         executionResult.setAndroid(true);
         coverageBasedSelection.setAndroid(true);
      }
      LOG.error("Commit not running");
      final CommitStaticSelection newCommitInfo = new CommitStaticSelection();
      newCommitInfo.setRunning(false);
      staticSelectionResult.getCommits().put(commit, newCommitInfo);
   }

   public boolean readInitialCommit() throws IOException, ParseException {
      changeManager = new ChangeManager(folders, iterator, executionConfig, dependencyManager.getExecutor());
      staticChangeHandler = new StaticChangeHandler(folders, executionConfig, dependencyManager);
      InitialCommitReader initialVersionReader = new InitialCommitReader(staticSelectionResult, dependencyManager, iterator);
      if (initialVersionReader.readInitialCommit()) {
         DependencyReaderUtil.write(staticSelectionResult, resultsFolders.getStaticTestSelectionFile());
         lastRunningVersion = iterator.getCommitName();

         if (testSelectionConfig.isGenerateTraces()) {
            generateInitialViews();
         }
         dependencyManager.cleanResultFolder();
         return true;
      } else {
         return false;
      }
   }

   private void generateInitialViews() throws IOException, ParseException {
      TestSet initialTests = staticSelectionResult.getInitialcommit().getInitialTests();
      TraceViewGenerator traceViewGenerator = new TraceViewGenerator(dependencyManager, folders, iterator.getCommitName(), traceFileMapping, kiekerConfig, testSelectionConfig);
      traceViewGenerator.generateViews(resultsFolders, initialTests);

      executionResult.getCommits().put(iterator.getCommitName(), new TestSet());
      coverageBasedSelection.getCommits().put(iterator.getCommitName(), new TestSet());
      twiceExecutableSelected.getCommits().put(iterator.getCommitName(), new TestSet());
   }

   public void readCompletedCommits(final StaticTestSelection initialdependencies, CommitComparatorInstance comparator) {
      changeManager = new ChangeManager(folders, iterator, executionConfig, dependencyManager.getExecutor());
      staticChangeHandler = new StaticChangeHandler(folders, executionConfig, dependencyManager);

      staticSelectionResult.setCommits(initialdependencies.getCommits());
      staticSelectionResult.setInitialcommit(initialdependencies.getInitialcommit());

      InitialCommitReader initialCommitReader = new InitialCommitReader(initialdependencies, dependencyManager, iterator);
      initialCommitReader.readCompletedCommits(comparator);
      DependencyReaderUtil.write(staticSelectionResult, resultsFolders.getStaticTestSelectionFile());
      lastRunningVersion = iterator.getCommitName();
   }

   public StaticTestSelection getDependencies() {
      return staticSelectionResult;
   }

   public ExecutionData getExecutionResult() {
      return executionResult;
   }

   public ExecutionData getCoverageBasedSelection() {
      return coverageBasedSelection;
   }

   public void setIterator(final CommitIterator reserveIterator) {
      this.iterator = reserveIterator;
   }

   public void setCoverageExecutions(final ExecutionData coverageExecutions) {
      coverageBasedSelection.setUrl(coverageExecutions.getUrl());
      coverageBasedSelection.setCommits(coverageExecutions.getCommits());
   }

   public void setExecutionData(final ExecutionData executions) {
      executionResult.setUrl(executions.getUrl());
      executionResult.setCommits(executions.getCommits());

      new OldTraceReader(traceFileMapping, staticSelectionResult, resultsFolders).addTraces();
   }

   public void setCoverageInfo(final CoverageSelectionInfo coverageInfo) {
      coverageSelectionInfo.getCommits().putAll(coverageInfo.getCommits());
   }

   public TestExecutor getExecutor() {
      return dependencyManager.getExecutor();
   }
}
