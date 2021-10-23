package de.dagere.peass.dependency.reader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.github.javaparser.ParseException;

import de.dagere.peass.ci.NonIncludedTestRemover;
import de.dagere.peass.config.DependencyConfig;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.ChangeManager;
import de.dagere.peass.dependency.DependencyManager;
import de.dagere.peass.dependency.analysis.data.ChangeTestMapping;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.Version;
import de.dagere.peass.dependency.traces.DiffFileGenerator;
import de.dagere.peass.dependency.traces.OneTraceGenerator;
import de.dagere.peass.dependency.traces.TraceFileMapping;
import de.dagere.peass.dependency.traces.coverage.CoverageBasedSelector;
import de.dagere.peass.dependency.traces.coverage.CoverageSelectionInfo;
import de.dagere.peass.dependency.traces.coverage.CoverageSelectionVersion;
import de.dagere.peass.dependency.traces.coverage.TraceCallSummary;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.vcs.VersionIterator;

/**
 * Shared functions for dependency reading, which are both used if dependencies are read fully or if one continues a dependency reading process.
 * 
 * @author reichelt
 *
 */
public class DependencyReader {

   private static final boolean DETAIL_DEBUG = true;

   private static final Logger LOG = LogManager.getLogger(DependencyReader.class);

   private final DependencyConfig dependencyConfig;
   protected final Dependencies dependencyResult = new Dependencies();
   private final ExecutionData executionResult = new ExecutionData();
   private final ExecutionData coverageBasedSelection = new ExecutionData();
   private final CoverageSelectionInfo coverageSelectionInfo = new CoverageSelectionInfo();
   protected final ResultsFolders resultsFolders;
   protected DependencyManager dependencyManager;
   protected final PeassFolders folders;
   protected VersionIterator iterator;
   protected String lastRunningVersion;
   private final VersionKeeper skippedNoChange;
   
   private final KiekerConfig kiekerConfig;
   private final ExecutionConfig executionConfig;
   private final EnvironmentVariables env;

   private final ChangeManager changeManager;
   private final DependencySizeRecorder sizeRecorder = new DependencySizeRecorder();
   private final TraceFileMapping mapping = new TraceFileMapping();

   public DependencyReader(final DependencyConfig dependencyConfig, final PeassFolders folders,
         final ResultsFolders resultsFolders, final String url, final VersionIterator iterator,
         final ChangeManager changeManager, final ExecutionConfig executionConfig, final KiekerConfig kiekerConfig, final EnvironmentVariables env) {
      this.dependencyConfig = dependencyConfig;
      this.resultsFolders = resultsFolders;
      this.iterator = iterator;
      this.folders = folders;
      this.skippedNoChange = new VersionKeeper(new File("/dev/null"));
      this.executionConfig = executionConfig;
      this.kiekerConfig = kiekerConfig;
      this.env = env;

      dependencyResult.setUrl(url);
      executionResult.setUrl(url);
      coverageBasedSelection.setUrl(url);

      this.changeManager = changeManager;
      
      if (!kiekerConfig.isUseKieker()) {
         throw new RuntimeException("Dependencies may only be read if Kieker is enabled!");
      }
   }

   /**
    * Starts reading dependencies
    * 
    * @param projectFolder
    * @param dependencyFile
    * @param url
    * @param iterator
    */
   public DependencyReader(final DependencyConfig dependencyConfig, final PeassFolders folders, final ResultsFolders resultsFolders, final String url,
         final VersionIterator iterator,
         final VersionKeeper skippedNoChange, final ExecutionConfig executionConfig, final KiekerConfig kiekerConfig, final EnvironmentVariables env) {
      this.dependencyConfig = dependencyConfig;
      this.resultsFolders = resultsFolders;
      this.iterator = iterator;
      this.folders = folders;
      this.skippedNoChange = skippedNoChange;
      this.executionConfig = executionConfig;
      this.kiekerConfig = kiekerConfig;
      this.env = env;

      dependencyResult.setUrl(url);

      changeManager = new ChangeManager(folders, iterator, executionConfig);
      
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
         lastRunningVersion = iterator.getTag();
         while (iterator.hasNextCommit()) {
            iterator.goToNextCommit();
            readVersion();
         }

         LOG.debug("Finished dependency-reading");
         return true;
      } catch (IOException | XmlPullParserException | InterruptedException | ParseException | ViewNotFoundException e) {
         e.printStackTrace();
         return false;
      }
   }

   public void readVersion() throws IOException, FileNotFoundException, XmlPullParserException, InterruptedException, ParseException, ViewNotFoundException {
      final int tests = analyseVersion(changeManager);
      DependencyReaderUtil.write(dependencyResult, resultsFolders.getDependencyFile());
      if (dependencyConfig.isGenerateViews()) {
         Constants.OBJECTMAPPER.writeValue(resultsFolders.getExecutionFile(), executionResult);
         if (dependencyConfig.isGenerateCoverageSelection()) {
            Constants.OBJECTMAPPER.writeValue(resultsFolders.getCoverageSelectionFile(), coverageBasedSelection);
            Constants.OBJECTMAPPER.writeValue(resultsFolders.getCoverageInfoFile(), coverageSelectionInfo);
         }
      }

      sizeRecorder.addVersionSize(dependencyManager.getDependencyMap().size(), tests);

      dependencyManager.getExecutor().deleteTemporaryFiles();
      TooBigLogCleaner.cleanXMLFolder(folders);
      TooBigLogCleaner.cleanTooBigLogs(folders, iterator.getTag());
   }

   /**
    * Determines the tests that may have got new dependencies, writes that changes (i.e. the tests that need to be run in that version) and re-runs the tests in order to get the
    * updated test dependencies.
    * 
    * @param dependencyFile
    * @param dependencyManager
    * @param dependencies
    * @param dependencyResult
    * @param version
    * @return
    * @throws IOException
    * @throws XmlPullParserException
    * @throws InterruptedException
    * @throws ViewNotFoundException
    * @throws ParseException
    */
   public int analyseVersion(final ChangeManager changeManager) throws IOException, XmlPullParserException, InterruptedException, ParseException, ViewNotFoundException {
      final String version = iterator.getTag();
      if (!dependencyConfig.isSkipProcessSuccessRuns()) {
         if (!dependencyManager.getExecutor().isVersionRunning(iterator.getTag())) {
            documentFailure(version);
            return 0;
         }
      }
      

      dependencyManager.getExecutor().loadClasses();

      final DependencyReadingInput input = new DependencyReadingInput(changeManager.getChanges(lastRunningVersion), lastRunningVersion);
      changeManager.saveOldClasses();
      lastRunningVersion = iterator.getTag();

      if (DETAIL_DEBUG) {
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "initialdependencies_" + version + ".json"), dependencyManager.getDependencyMap());
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "changes_" + version + ".json"), input.getChanges());
      }

      if (input.getChanges().size() > 0) {
         return analyseChanges(version, input);
      } else {
         addEmptyVersionData(version, input);
         return 0;
      }
   }

   private void addEmptyVersionData(final String version, final DependencyReadingInput input) {
      Version emptyVersion = new Version();
      emptyVersion.setJdk(dependencyManager.getExecutor().getJDKVersion());
      emptyVersion.setRunning(true);
      emptyVersion.setPredecessor(input.getPredecessor());
      dependencyResult.getVersions().put(version, emptyVersion);
      if (dependencyConfig.isGenerateViews()) {
         executionResult.addEmptyVersion(version, null);
         coverageBasedSelection.addEmptyVersion(version, null);
      }
      skippedNoChange.addVersion(version, "No Change at all");
   }

   private int analyseChanges(final String version, final DependencyReadingInput input)
         throws IOException, JsonGenerationException, JsonMappingException, XmlPullParserException, InterruptedException, ParseException, ViewNotFoundException {
      final Version newVersionInfo = handleStaticAnalysisChanges(version, input);

      if (!dependencyConfig.isDoNotUpdateDependencies()) {
         TraceChangeHandler traceChangeHandler = new TraceChangeHandler(dependencyManager, folders, executionConfig, version);
         traceChangeHandler.handleTraceAnalysisChanges(newVersionInfo);

         if (dependencyConfig.isGenerateViews()) {
            executionResult.addEmptyVersion(version, newVersionInfo.getPredecessor());
            TraceViewGenerator traceViewGenerator = new TraceViewGenerator(dependencyManager, folders, version, mapping);
            traceViewGenerator.generateViews(resultsFolders, newVersionInfo.getTests());

            DiffFileGenerator diffGenerator = new DiffFileGenerator(resultsFolders.getVersionDiffFolder(version));
            diffGenerator.generateAllDiffs(version, newVersionInfo, diffGenerator, mapping, executionResult);

            if (dependencyConfig.isGenerateCoverageSelection()) {
               generateCoverageBasedSelection(version, newVersionInfo);
            }
         }
      } else {
         LOG.debug("Not updating dependencies since doNotUpdateDependencies was set - only returning dependencies based on changed classes");
      }
      dependencyResult.getVersions().put(version, newVersionInfo);

      final int changedClazzCount = calculateChangedClassCount(newVersionInfo);
      return changedClazzCount;
   }

   private void generateCoverageBasedSelection(final String version, final Version newVersionInfo) throws IOException, JsonParseException, JsonMappingException {
      List<TraceCallSummary> summaries = new LinkedList<>();
      for (TestCase testcase : newVersionInfo.getTests().getTests()) {
         List<File> traceFiles = mapping.getTestcaseMap(testcase);
         if (traceFiles != null && traceFiles.size() > 1) {
            File oldFile = new File(traceFiles.get(0).getAbsolutePath() + OneTraceGenerator.SUMMARY);
            File newFile = new File(traceFiles.get(1).getAbsolutePath() + OneTraceGenerator.SUMMARY);
            TraceCallSummary oldSummary = Constants.OBJECTMAPPER.readValue(oldFile, TraceCallSummary.class);
            TraceCallSummary newSummary = Constants.OBJECTMAPPER.readValue(newFile, TraceCallSummary.class);
            summaries.add(oldSummary);
            summaries.add(newSummary);
            LOG.info("Found traces for {}", testcase);
         } else {
            LOG.info("Trace files missing for {}", testcase);
         }
      }

      for (ChangedEntity change : newVersionInfo.getChangedClazzes().keySet()) {
         LOG.info("Change: {}", change.toString());
         LOG.info("Parameters: {}", change.getParametersPrintable());
      }

      CoverageSelectionVersion selected = CoverageBasedSelector.selectBasedOnCoverage(summaries, newVersionInfo.getChangedClazzes().keySet());
      for (TraceCallSummary traceCallSummary : selected.getTestcases().values()) {
         if (traceCallSummary.isSelected()) {
            coverageBasedSelection.addCall(version, traceCallSummary.getTestcase());
         }
      }
      coverageSelectionInfo.getVersions().put(version, selected);
   }

   private int calculateChangedClassCount(final Version newVersionInfo) {
      final int changedClazzCount = newVersionInfo.getChangedClazzes().values().stream().mapToInt(value -> {
         return value.getTestcases().values().stream().mapToInt(list -> list.size()).sum();
      }).sum();
      return changedClazzCount;
   }

   private Version handleStaticAnalysisChanges(final String version, final DependencyReadingInput input) throws IOException, JsonGenerationException, JsonMappingException {
      final ChangeTestMapping changeTestMap = dependencyManager.getDependencyMap().getChangeTestMap(input.getChanges()); // tells which tests need to be run, and
      // because of which change they need to be run
      LOG.debug("Change test mapping (without added tests): " + changeTestMap);

      handleAddedTests(input, changeTestMap);

      if (DETAIL_DEBUG)
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "changetest_" + version + ".json"), changeTestMap);

      final Version newVersionInfo = DependencyReaderUtil.createVersionFromChangeMap(input.getChanges(), changeTestMap);
      newVersionInfo.setJdk(dependencyManager.getExecutor().getJDKVersion());
      newVersionInfo.setPredecessor(input.getPredecessor());

      if (DETAIL_DEBUG) {
         Constants.OBJECTMAPPER.writeValue(new File(folders.getDebugFolder(), "versioninfo_" + version + ".json"), newVersionInfo);
      }
      return newVersionInfo;
   }

   private void handleAddedTests(final DependencyReadingInput input, final ChangeTestMapping changeTestMap) {
      dependencyManager.getTestTransformer().determineVersions(dependencyManager.getExecutor().getModules().getModules());
      for (ClazzChangeData changedEntry : input.getChanges().values()) {
         if (!changedEntry.isOnlyMethodChange()) {
            for (ChangedEntity change : changedEntry.getChanges()) {
               File moduleFolder = new File(folders.getProjectFolder(), change.getModule());
               List<TestCase> addedTests = dependencyManager.getTestTransformer().getTestMethodNames(moduleFolder, change);
               for (TestCase added : addedTests) {
                  if (NonIncludedTestRemover.isTestIncluded(added, executionConfig.getIncludes())) {
                     changeTestMap.addChangeEntry(change, added.toEntity());
                  }
               }
            }
         }
      }
   }

   public void documentFailure(final String version) {
      if (dependencyManager.getExecutor().isAndroid()) {
         dependencyResult.setAndroid(true);
         executionResult.setAndroid(true);
      }
      LOG.error("Version not running");
      final Version newVersionInfo = new Version();
      newVersionInfo.setRunning(false);
      dependencyResult.getVersions().put(version, newVersionInfo);
   }

   public boolean readInitialVersion() throws IOException, InterruptedException, XmlPullParserException, ParseException, ViewNotFoundException {
      dependencyManager = new DependencyManager(folders, executionConfig, kiekerConfig, env);
      InitialVersionReader initialVersionReader = new InitialVersionReader(dependencyResult, dependencyManager, iterator);
      if (initialVersionReader.readInitialVersion()) {
         DependencyReaderUtil.write(dependencyResult, resultsFolders.getDependencyFile());
         lastRunningVersion = iterator.getTag();

         if (dependencyConfig.isGenerateViews()) {
            generateInitialViews();
         }
         dependencyManager.cleanResultFolder();
         return true;
      } else {
         return false;
      }
   }

   private void generateInitialViews() throws IOException, XmlPullParserException, ParseException, ViewNotFoundException, InterruptedException {
      TestSet initialTests = dependencyResult.getInitialversion().getInitialTests();
      TraceViewGenerator traceViewGenerator = new TraceViewGenerator(dependencyManager, folders, iterator.getTag(), mapping);
      traceViewGenerator.generateViews(resultsFolders, initialTests);

      executionResult.getVersions().put(iterator.getTag(), new TestSet());
   }

   public void readCompletedVersions(final Dependencies initialdependencies) {
      dependencyManager = new DependencyManager(folders, executionConfig, kiekerConfig, env);

      dependencyResult.setVersions(initialdependencies.getVersions());
      dependencyResult.setInitialversion(initialdependencies.getInitialversion());

      InitialVersionReader initialVersionReader = new InitialVersionReader(initialdependencies, dependencyManager, iterator);
      initialVersionReader.readCompletedVersions();
      DependencyReaderUtil.write(dependencyResult, resultsFolders.getDependencyFile());
      lastRunningVersion = iterator.getTag();
   }

   public Dependencies getDependencies() {
      return dependencyResult;
   }

   public ExecutionData getExecutionResult() {
      return executionResult;
   }

   public ExecutionData getCoverageBasedSelection() {
      return coverageBasedSelection;
   }

   public void setIterator(final VersionIterator reserveIterator) {
      this.iterator = reserveIterator;
   }

   public void setCoverageExecutions(final ExecutionData coverageExecutions) {
      coverageBasedSelection.setUrl(coverageExecutions.getUrl());
      coverageBasedSelection.setVersions(coverageExecutions.getVersions());
   }

   public void setExecutionData(final ExecutionData executions) {
      executionResult.setUrl(executions.getUrl());
      executionResult.setVersions(executions.getVersions());

      new OldTraceReader(mapping, dependencyResult, resultsFolders).addTraces();
   }

   public void setCoverageInfo(final CoverageSelectionInfo coverageInfo) {
      coverageSelectionInfo.getVersions().putAll(coverageInfo.getVersions());
   }
}
