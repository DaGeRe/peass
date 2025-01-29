package de.dagere.peass.measurement.rca.searcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.measurement.rca.CausePersistenceManager;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.CauseTester;
import de.dagere.peass.measurement.rca.RCAMeasurementAdder;
import de.dagere.peass.measurement.rca.analyzer.CompleteTreeAnalyzer;
import de.dagere.peass.measurement.rca.analyzer.TreeAnalyzer;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kieker.BothTreeReader;
import de.dagere.peass.measurement.rca.treeanalysis.AllDifferingDeterminer;
import de.dagere.peass.measurement.utils.sjsw.SjswCctConverter;
import de.dagere.peass.vcs.GitUtils;
import io.github.terahidro2003.result.tree.StackTraceTreeNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.config.FixedCommitConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.MeasurementStrategy;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.execution.processutils.ProcessBuilderHelper;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.dependencyprocessors.SamplingRunner;
import de.dagere.peass.measurement.dependencyprocessors.helper.ProgressWriter;
import de.dagere.peass.measurement.organize.FolderDeterminer;
import de.dagere.peass.measurement.organize.ResultOrganizer;
import de.dagere.peass.testtransformation.TestTransformer;
import de.dagere.peass.utils.Constants;
import io.github.terahidro2003.config.Config;
import io.github.terahidro2003.result.SamplerResultsProcessor;
import io.github.terahidro2003.samplers.asyncprofiler.MeasurementIdentifier;

public class SamplingCauseSearcher implements ICauseSearcher {

   private static final Logger LOG = LogManager.getLogger(SamplingCauseSearcher.class);

   private final TestMethodCall testcase;
   protected final MeasurementConfig configuration;
   protected final CauseSearchFolders folders;
   private ResultOrganizer currentOrganizer;
   protected final EnvironmentVariables env;
   protected final CauseSearcherConfig causeSearcherConfig;
   private final CausePersistenceManager persistenceManager;
   private final BothTreeReader reader;

   protected long currentChunkStart = 0;

   public SamplingCauseSearcher(TestMethodCall testcase, MeasurementConfig configuration, CauseSearchFolders folders,
         EnvironmentVariables env, CauseSearcherConfig causeSearcherConfig,
         final BothTreeReader reader) {
      this.testcase = testcase;
      this.configuration = configuration;
      this.folders = folders;
      this.env = env;
      this.causeSearcherConfig = causeSearcherConfig;
      this.persistenceManager = new CausePersistenceManager(causeSearcherConfig, configuration, folders);
      this.reader = reader;
   }

   @Override
   public Set<MethodCall> search() {
      FixedCommitConfig fixedCommitConfig = configuration.getFixedCommitConfig();
      LOG.info("Executing test " + testcase.getClazz() + " " + testcase.getMethod() + " in commits {} and {}", fixedCommitConfig.getCommitOld(),
            fixedCommitConfig.getCommit());
      new FolderDeterminer(folders).testResultFolders(fixedCommitConfig.getCommit(), fixedCommitConfig.getCommitOld(), testcase);

      final File logFolder = folders.getRCALogFolder(configuration.getFixedCommitConfig().getCommit(), testcase, 0);
      Set<MethodCall> result = new HashSet<>();
      try (ProgressWriter writer = new ProgressWriter(folders.getProgressFile(), configuration.getVms())) {
         result = evaluateSimple(testcase, logFolder, writer);
      }

      if (result.isEmpty()) {
         throw new RuntimeException("Result is empty");
      }

      return result;
   }

   private Set<MethodCall> evaluateSimple(TestMethodCall testcase2, File logFolder, ProgressWriter writer) {
      currentChunkStart = System.currentTimeMillis();

      MeasurementIdentifier measurementIdentifier = new MeasurementIdentifier();
      String outputPath = logFolder.getAbsolutePath() + "/sjsw-results";

      Config sjswConfiguration = Config.builder()
            .autodownloadProfiler()
            .outputPathWithIdentifier(outputPath, measurementIdentifier)
            .frequency(1)
            .jfrEnabled(true)
            .build();

      SamplerResultsProcessor processor = new SamplerResultsProcessor();

      for (int finishedVMs = 0; finishedVMs < configuration.getVms(); finishedVMs++) {
         long comparisonStart = System.currentTimeMillis();

         runOneComparison(logFolder, testcase, finishedVMs, sjswConfiguration);

         long durationInSeconds = (System.currentTimeMillis() - comparisonStart) / 1000;
         writer.write(durationInSeconds, finishedVMs);

         betweenVMCooldown();
      }

      return analyseSamplingResults(processor, measurementIdentifier, testcase2, configuration.getVms());
   }

   private Set<MethodCall> analyseSamplingResults(SamplerResultsProcessor processor, MeasurementIdentifier identifier, TestMethodCall testcase, int vms) {
      File resultDir = retrieveSamplingResultsDirectory(identifier);
      Path resultsPath = resultDir.toPath();
      var commits = getVersions();

      StackTraceTreeNode commitBAT = retrieveBatForCommit(commits[1], processor, resultsPath);
      StackTraceTreeNode predecessorBAT = retrieveBatForCommit(commits[0], processor, resultsPath);

      // Convert BAT to CallTreeNode for both commits
      CallTreeNode root = null;
      root = SjswCctConverter.convertCallContextTreeToCallTree(commitBAT, predecessorBAT, root, commits[1], commits[0], vms);

      if (root == null) {
         throw new RuntimeException("CallTreeNode was null after attempted conversion from SJSW structure.");
      }

      // Persist CallTreeNode
      persistBasicCallTreeNode(root);
      printCallTreeNode(root);
      System.out.println();
      printCallTreeNode(root.getOtherCommitNode());

      CompleteTreeAnalyzer completeTreeAnalyzer = new CompleteTreeAnalyzer(root, root.getOtherCommitNode());

      Set<MethodCall> differentMethods = getDifferingMethodCalls(root, root.getOtherCommitNode());
      return differentMethods;
   }

   public static void printCallTreeNode(CallTreeNode root) {
      printCallTreeNodeTreeRecursive(root, "", false);
   }

   public static void printCallTreeNodeTreeRecursive(CallTreeNode node, String prefix, boolean isLast) {
      if (node.getMethod() != null) {
         System.out.println(prefix + (isLast ? "└────── " : "├────── ") + node.getMethod() +
                 " Keys: [" + node.getKeys());
      }

      List<CallTreeNode> children = node.getChildren();
      for (int i = 0; i < children.size(); i++) {
         printCallTreeNodeTreeRecursive(children.get(i), prefix + (isLast ? "    " : "│   "), i == children.size() - 1);
      }
   }

   private void persistBasicCallTreeNode(CallTreeNode node) {
      String outputFile = folders.getMeasureLogFolder().getAbsoluteFile() + "/calltreenode_serialized" + UUID.randomUUID() + ".json";
      try {
         Constants.OBJECTMAPPER.writeValue(new File(outputFile), node);
      } catch (IOException e) {
         LOG.error("Failed to serialize call tree node {}", node, e);
      }
   }

   private Set<MethodCall> getDifferingMethodCalls(CallTreeNode currentRoot, CallTreeNode rootPredecessor) {
      // Define tree analyzer
      var creator = new TreeAnalyzerCreator() {
         @Override
         public TreeAnalyzer getAnalyzer(final BothTreeReader reader, final CauseSearcherConfig config) {
            return new CompleteTreeAnalyzer(currentRoot, rootPredecessor);
         }
      };
      final TreeAnalyzer analyzer = creator.getAnalyzer(reader, causeSearcherConfig);
      final List<CallTreeNode> predecessorNodeList = analyzer.getMeasurementNodesPredecessor();
      final List<CallTreeNode> includableNodes = getIncludableNodes(predecessorNodeList);

      if (includableNodes.isEmpty()) {
         throw new RuntimeException("Tried to analyze empty node list");
      }

      applyMeasurementToDefinedTree(includableNodes, rootPredecessor);

      return convertToChangedEntitites(includableNodes);
   }

   private void applyMeasurementToDefinedTree(List<CallTreeNode> differingNodes, CallTreeNode rootPredecessor) {
      final AllDifferingDeterminer allSearcher = new AllDifferingDeterminer(differingNodes, causeSearcherConfig, configuration);
      allSearcher.calculateDiffering();

      RCAMeasurementAdder measurementReader = new RCAMeasurementAdder(persistenceManager, differingNodes);
      measurementReader.addAllMeasurements(rootPredecessor);

      differingNodes.addAll(allSearcher.getLevelDifferentPredecessor());

      persistenceManager.writeTreeState();
   }

   private Set<MethodCall> convertToChangedEntitites(List<CallTreeNode> differingNodes) {
      final Set<MethodCall> changed = new TreeSet<>();
      differingNodes.forEach(node -> {
         changed.add(node.toEntity());
      });
      return changed;
   }

   private List<CallTreeNode> getIncludableNodes(final List<CallTreeNode> predecessorNodeList) {
      final List<CallTreeNode> includableNodes;
      if (causeSearcherConfig.useCalibrationRun()) {
         includableNodes = getAnalysableNodes(predecessorNodeList);
      } else {
         includableNodes = predecessorNodeList;
      }

      LOG.debug("Analyzable: {} / {}", includableNodes.size(), predecessorNodeList.size());
      return includableNodes;
   }

   private List<CallTreeNode> getAnalysableNodes(final List<CallTreeNode> predecessorNodeList) {
      final MeasurementConfig config = new MeasurementConfig(1, configuration.getFixedCommitConfig().getCommit(), configuration.getFixedCommitConfig().getCommitOld());
      config.setIterations(configuration.getIterations());
      config.setRepetitions(configuration.getRepetitions());
      config.setWarmup(configuration.getWarmup());
      config.getKiekerConfig().setUseKieker(true);

      List<String> commits = GitUtils.getCommits(folders.getProjectFolder(), true, true);
      CommitComparatorInstance comparator = new CommitComparatorInstance(commits);

      final CauseTester calibrationMeasurer = new CauseTester(folders, config, causeSearcherConfig, env, comparator);
      final AllDifferingDeterminer calibrationRunner = new AllDifferingDeterminer(predecessorNodeList, causeSearcherConfig, config);
      calibrationMeasurer.measureCommit(predecessorNodeList);
      final List<CallTreeNode> includableByMinTime = calibrationRunner.getIncludableNodes();
      return includableByMinTime;
   }

   private StackTraceTreeNode retrieveBatForCommit(String commit, SamplerResultsProcessor processor, Path resultsPath) {
      List<File> commitJfrs = processor.listJfrMeasurementFiles(resultsPath, List.of(commit));
      StackTraceTreeNode tree = processor.getTreeFromJfr(commitJfrs, commit);
      String normalizedMethodName = testcase.getMethod().substring(testcase.getMethod().lastIndexOf('#') + 1);
      StackTraceTreeNode filteredTestcaseTree = processor.filterTestcaseSubtree(normalizedMethodName, tree);
      filteredTestcaseTree.printTree();
      return filteredTestcaseTree;
   }

   private File retrieveSamplingResultsDirectory(MeasurementIdentifier identifier) {
      final File logFolder = folders.getRCALogFolder(configuration.getFixedCommitConfig().getCommit(), testcase, 0);
      String outputPath = logFolder.getAbsolutePath() + "/sjsw-results";
      return new File(outputPath + "/measurement_" + identifier.getUuid().toString());
   }

   public void runOneComparison(final File logFolder, final TestMethodCall testcase, final int vmid, final Config sjswConfiguration) {
      String[] commits = getVersions();

      if (configuration.getMeasurementStrategy().equals(MeasurementStrategy.SEQUENTIAL)) {
         LOG.info("Running sequential");
         runSequential(logFolder, testcase, vmid, commits, sjswConfiguration);
      } else if (configuration.getMeasurementStrategy().equals(MeasurementStrategy.PARALLEL)) {
         LOG.info("Running parallel");
         runParallel(logFolder, testcase, vmid, commits);
      }
   }

   private void runParallel(File logFolder, TestMethodCall testcase2, int vmid, String[] commits) {
      throw new RuntimeException("Not implemented yet");
   }

   private void runSequential(File logFolder, TestMethodCall testcase2, int vmid, String[] commits, Config config) {
      currentOrganizer = new ResultOrganizer(folders, configuration.getFixedCommitConfig().getCommit(), currentChunkStart, configuration.getKiekerConfig().isUseKieker(),
            configuration.isSaveAll(),
            testcase, configuration.getAllIterations());
      for (String commit : commits) {
         runOnce(testcase, commit, vmid, logFolder, config);
      }
   }

   private void runOnce(final TestMethodCall testcase, final String commit, final int vmid, final File logFolder, final Config config) {
      final TestExecutor testExecutor = getExecutor(folders, commit);
      final SamplingRunner runner = new SamplingRunner(folders, testExecutor, getCurrentOrganizer(), this, config);
      runner.runOnce(testcase, commit, vmid, logFolder);
   }

   protected synchronized TestExecutor getExecutor(final PeassFolders currentFolders, final String commit) {
      TestTransformer transformer = ExecutorCreator.createTestTransformer(currentFolders, configuration.getExecutionConfig(), configuration);
      final TestExecutor testExecutor = ExecutorCreator.createExecutor(currentFolders, transformer, env);
      return testExecutor;
   }

   protected void betweenVMCooldown() {
      if (configuration.isCallSyncBetweenVMs()) {
         ProcessBuilderHelper.syncToHdd();
      }
      try {
         Thread.sleep(configuration.getWaitTimeBetweenVMs());
      } catch (InterruptedException e) {
         throw new RuntimeException(e);
      }
   }

   public ResultOrganizer getCurrentOrganizer() {
      return currentOrganizer;
   }

   private String[] getVersions() {
      String commits[] = new String[2];
      commits[0] = configuration.getFixedCommitConfig().getCommitOld().equals("HEAD~1") ? configuration.getFixedCommitConfig().getCommit() + "~1"
            : configuration.getFixedCommitConfig().getCommitOld();
      commits[1] = configuration.getFixedCommitConfig().getCommit();
      return commits;
   }
}
