package de.dagere.peass.measurement.rca.searcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.utils.sjsw.SjswCctConverter;
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
import io.github.terahidro2003.config.Config;
import io.github.terahidro2003.result.SamplerResultsProcessor;
import io.github.terahidro2003.samplers.asyncprofiler.MeasurementIdentifier;

public class SamplingCauseSearcher implements ICauseSearcher {

   private static final Logger LOG = LogManager.getLogger(SamplingCauseSearcher.class);

   private final TestMethodCall testcase;
   protected final MeasurementConfig configuration;
   protected final PeassFolders folders;
   private ResultOrganizer currentOrganizer;
   protected final EnvironmentVariables env;
   
   protected long currentChunkStart = 0;

   public SamplingCauseSearcher(TestMethodCall testcase, MeasurementConfig configuration, PeassFolders folders, EnvironmentVariables env) {
      this.testcase = testcase;
      this.configuration = configuration;
      this.folders = folders;
      this.env = env;
   }

   @Override
   public Set<MethodCall> search() {
      FixedCommitConfig fixedCommitConfig = configuration.getFixedCommitConfig();
      LOG.info("Executing test " + testcase.getClazz() + " " + testcase.getMethod() + " in commits {} and {}", fixedCommitConfig.getCommitOld(),
            fixedCommitConfig.getCommit());
      new FolderDeterminer(folders).testResultFolders(fixedCommitConfig.getCommit(), fixedCommitConfig.getCommitOld(), testcase);

      final File logFolder = folders.getMeasureLogFolder(configuration.getFixedCommitConfig().getCommit(), testcase);
      try (ProgressWriter writer = new ProgressWriter(folders.getProgressFile(), configuration.getVms())) {
         evaluateSimple(testcase, logFolder, writer);
      }

      throw new RuntimeException("Not implemented yet");
   }

   private void evaluateSimple(TestMethodCall testcase2, File logFolder, ProgressWriter writer) {
      currentChunkStart = System.currentTimeMillis();

      MeasurementIdentifier measurementIdentifier = new MeasurementIdentifier();
      String outputPath = logFolder.getAbsolutePath() + "/sjsw-results";
      
      Config sjswConfiguration = Config.builder()
               .autodownloadProfiler()
               .outputPathWithIdentifier(outputPath, measurementIdentifier)
               .frequency(100)
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

      analyseSamplingResults(processor, measurementIdentifier, testcase2, configuration.getVms());
   }

   private void analyseSamplingResults(SamplerResultsProcessor processor, MeasurementIdentifier identifier, TestMethodCall testcase, int vms) {
      File resultDir = retrieveSamplingResultsDirectory(identifier);
      Path resultsPath = resultDir.toPath();
      var commits = getVersions();

      List<CallTreeNode> vmNodes = new ArrayList<>();

      for (int i = 0; i<commits.length; i++) {
         // Build BATs for both commits
         StackTraceTreeNode BAT = retrieveBatForCommit(commits[i], processor, resultsPath);

         // Convert BAT to CallTreeNode for both commits
         CallTreeNode root = null;
         root = SjswCctConverter.convertCallContextTreeToCallTree(BAT, root, commits[i], null,  vms);
         vmNodes.add(root);

         if (root == null) {
            throw new RuntimeException("CallTreeNode was null after attempted conversion from SJSW structure.");
         }
      }

      // Persist CallTreeNode
      ObjectMapper objectMapper = new ObjectMapper();
      for (CallTreeNode node : vmNodes) {
         String outputFile = folders.getMeasureLogFolder().getAbsoluteFile() + "/calltreenode_serialized" + UUID.randomUUID() + ".json";
         try {
            objectMapper.writeValue(new File(outputFile), node);
         } catch (IOException e) {
            LOG.error("Failed to serialize call tree node {}", node, e);
         }
      }

      // Map CallTreeNodes
   }

   private StackTraceTreeNode retrieveBatForCommit(String commit, SamplerResultsProcessor processor, Path resultsPath) {
      List<File> commitJfrs = processor.listJfrMeasurementFiles(resultsPath, List.of(commit));
      StackTraceTreeNode tree = processor.getTreeFromJfr(commitJfrs, commit);
      StackTraceTreeNode filteredTestcaseTree = processor.filterTestcaseSubtree(testcase.getMethod(), tree);
      filteredTestcaseTree.printTree();
      return filteredTestcaseTree;
   }

   private File retrieveSamplingResultsDirectory(MeasurementIdentifier identifier) {
      final File logFolder = folders.getMeasureLogFolder(configuration.getFixedCommitConfig().getCommit(), testcase);
      String outputPath = logFolder.getAbsolutePath() + "/sjsw-results";
      return new File(outputPath + "/measurement_" +identifier.getUuid().toString());
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
