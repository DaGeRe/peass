package de.dagere.peass.measurement.rca;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.dependencyprocessors.AdaptiveTester;
import de.dagere.peass.measurement.dependencyprocessors.helper.EarlyBreakDecider;
import de.dagere.peass.measurement.dependencyprocessors.helper.ProgressWriter;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.kieker.KiekerResultReader;
import de.dagere.peass.testtransformation.TestTransformer;

/**
 * Measures method calls adaptively instrumented by Kieker
 * 
 * @author reichelt
 *
 */
public class CauseTester extends AdaptiveTester {

   private static final Logger LOG = LogManager.getLogger(CauseTester.class);

   private Set<CallTreeNode> includedNodes;
   private Set<String> includedPattern;
   private final TestMethodCall testcase;
   private final CauseSearcherConfig causeConfig;
   private final CauseSearchFolders folders;
   private int levelId = 0;

   public CauseTester(final CauseSearchFolders project, final MeasurementConfig measurementConfig, final CauseSearcherConfig causeConfig, final EnvironmentVariables env,
         CommitComparatorInstance comparator) {
      super(project, measurementConfig, env, comparator);
      this.testcase = causeConfig.getTestCase();
      this.causeConfig = causeConfig;
      this.folders = project;
   }

   public void measureVersion(final List<CallTreeNode> nodes) {
      includedNodes = prepareNodes(nodes);
      evaluate(causeConfig.getTestCase());
      if (!getCurrentOrganizer().isSuccess()) {
         boolean shouldBreak = reduceExecutions(false, configuration.getIterations() / 2);
         configuration.setIterations(configuration.getIterations() / 2);
         if (shouldBreak) {
            throw new RuntimeException("Execution took too long, Iterations: " + configuration.getIterations()
                  + " Warmup: " + configuration.getWarmup()
                  + " Repetitions: " + configuration.getRepetitions());
         }
      } else {
         getDurations(levelId);
      }
      cleanup(levelId);
      levelId++;
   }

   private Set<CallTreeNode> prepareNodes(final List<CallTreeNode> nodes) {
      final Set<CallTreeNode> includedNodes = new HashSet<CallTreeNode>();
      nodes.forEach(node -> {
         if (!node.getConfig().getKiekerConfig().isMeasureAdded()) {
            if (!node.getKiekerPattern().equals(CauseSearchData.ADDED) && !node.getOtherKiekerPattern().equals(CauseSearchData.ADDED)) {
               includedNodes.add(node);
            }
         } else {
            includedNodes.add(node);
         }
      });

      nodes.forEach(node -> node.initCommitData());
      return includedNodes;
   }

   @Override
   public void evaluate(final TestMethodCall testcase) {
      LOG.debug("Adaptive execution: " + includedNodes);

      initEvaluation(testcase);

      final File logFolder = folders.getRCALogFolder(configuration.getFixedCommitConfig().getCommit(), testcase, levelId);

      try (ProgressWriter writer = new ProgressWriter(folders.getProgressFile(), configuration.getVms())) {
         evaluateWithAdaption(testcase, logFolder, writer);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected synchronized TestExecutor getExecutor(final PeassFolders temporaryFolders, final String commit) {
      final TestExecutor testExecutor = super.getExecutor(temporaryFolders, commit);
      TestTransformer testTransformer = testExecutor.getTestTransformer();
      testTransformer.setIgnoreEOIs(causeConfig.isIgnoreEOIs());
      PatternSetGenerator patternSetGenerator = new PatternSetGenerator(configuration.getFixedCommitConfig(), testcase);
      includedPattern = patternSetGenerator.generatePatternSet(includedNodes, commit);
      final HashSet<String> includedMethodPattern = new HashSet<>(includedPattern);
      testExecutor.setIncludedMethods(includedMethodPattern);
      return testExecutor;
   }

   @Override
   public boolean checkIsDecidable(final TestMethodCall testcase, final int vmid) {
      getDurationsVersion(configuration.getFixedCommitConfig().getCommit());
      getDurationsVersion(configuration.getFixedCommitConfig().getCommitOld());
      boolean allDecidable = super.checkIsDecidable(testcase, vmid);
      LOG.debug("Super decidable: {}", allDecidable);
      for (final CallTreeNode includedNode : includedNodes) {
         allDecidable &= checkLevelDecidable(vmid, allDecidable, includedNode);
      }
      LOG.debug("Level decideable: {}", allDecidable);
      return allDecidable;
   }

   private boolean checkLevelDecidable(final int vmid, final boolean allDecidable, final CallTreeNode includedNode) {
      final SummaryStatistics statisticsOld = includedNode.getStatistics(configuration.getFixedCommitConfig().getCommitOld());
      final SummaryStatistics statistics = includedNode.getStatistics(configuration.getFixedCommitConfig().getCommit());
      final EarlyBreakDecider decider = new EarlyBreakDecider(configuration, statisticsOld, statistics);
      final boolean nodeDecidable = decider.isBreakPossible(vmid);
      LOG.debug("{} decideable: {}", includedNode.getKiekerPattern(), allDecidable);
      LOG.debug("Old: {} {} Current: {} {}", statisticsOld.getMean(), statisticsOld.getStandardDeviation(),
            statistics.getMean(), statistics.getStandardDeviation());
      return nodeDecidable;
   }

   @Override
   public void handleKiekerResults(final String commit, final File commitResultFolder) {
      if (getCurrentOrganizer().testSuccess(commit)) {
         LOG.info("Did succeed in measurement - analyse values");

         boolean isOtherVersion = commit.equals(configuration.getFixedCommitConfig().getCommit());
         final KiekerResultReader kiekerResultReader = new KiekerResultReader(configuration.getKiekerConfig().isUseAggregation(), configuration.getKiekerConfig().getRecord(),
               includedNodes, commit, testcase,
               isOtherVersion);
         kiekerResultReader.setConsiderNodePosition(!configuration.getKiekerConfig().isUseAggregation());

         kiekerResultReader.readResults(commitResultFolder);
      } else {
         LOG.info("Did not success in measurement");
      }

   }

   public void setIncludedMethods(final Set<CallTreeNode> children) {
      includedNodes = children;
   }

   public void getDurations(final int levelId) {
      getDurationsVersion(configuration.getFixedCommitConfig().getCommit());
      getDurationsVersion(configuration.getFixedCommitConfig().getCommitOld());
   }

   public void cleanup(final int levelId) {
      organizeMeasurements(levelId, configuration.getFixedCommitConfig().getCommit(), configuration.getFixedCommitConfig().getCommit());
      organizeMeasurements(levelId, configuration.getFixedCommitConfig().getCommit(), configuration.getFixedCommitConfig().getCommitOld());
   }

   private void organizeMeasurements(final int levelId, final String mainCommit, final String commit) {
      final File testcaseFolder = folders.getFullResultFolder(testcase, mainCommit, commit);
      final File commitFolder = new File(folders.getArchiveResultFolder(mainCommit, testcase), commit);
      if (!commitFolder.exists()) {
         commitFolder.mkdir();
      }
      final File adaptiveRunFolder = new File(commitFolder, "" + levelId);
      try {
         FileUtils.moveDirectory(testcaseFolder, adaptiveRunFolder);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private void getDurationsVersion(final String commit) {
      includedNodes.forEach(node -> node.createStatistics(commit));
   }

   public void setCurrentVersion(final String commit) {
      configuration.getFixedCommitConfig().setCommit(commit);
      configuration.getFixedCommitConfig().setCommitOld(commit + "~1");
   }

}
