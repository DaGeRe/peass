package de.dagere.peass.measurement.rca;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.dependencyprocessors.AdaptiveTester;
import de.dagere.peass.measurement.dependencyprocessors.helper.EarlyBreakDecider;
import de.dagere.peass.measurement.dependencyprocessors.helper.ProgressWriter;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kieker.KiekerResultReader;
import de.dagere.peass.testtransformation.TestTransformer;
import kieker.analysis.exception.AnalysisConfigurationException;

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
   private final TestCase testcase;
   private final CauseSearcherConfig causeConfig;
   private final CauseSearchFolders folders;
   private int levelId = 0;

   public CauseTester(final CauseSearchFolders project, final MeasurementConfig measurementConfig, final CauseSearcherConfig causeConfig, final EnvironmentVariables env, CommitComparatorInstance comparator) {
      super(project, measurementConfig, env, comparator);
      this.testcase = causeConfig.getTestCase();
      this.causeConfig = causeConfig;
      this.folders = project;
   }

   public void measureVersion(final List<CallTreeNode> nodes)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException {
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
      includedNodes.addAll(nodes);
      nodes.forEach(node -> node.initVersions());
      return includedNodes;
   }

   @Override
   public void evaluate(final TestCase testcase) throws IOException, InterruptedException, XmlPullParserException {
      LOG.debug("Adaptive execution: " + includedNodes);

      initEvaluation(testcase);

      final File logFolder = folders.getRCALogFolder(configuration.getExecutionConfig().getCommit(), testcase, levelId);

      try (ProgressWriter writer = new ProgressWriter(folders.getProgressFile(), configuration.getVms())) {
         evaluateWithAdaption(testcase, logFolder, writer);
      }
   }

   @Override
   protected synchronized TestExecutor getExecutor(final PeassFolders temporaryFolders, final String version) {
      final TestExecutor testExecutor = super.getExecutor(temporaryFolders, version);
      TestTransformer testTransformer = testExecutor.getTestTransformer();
      testTransformer.setIgnoreEOIs(causeConfig.isIgnoreEOIs());
      PatternSetGenerator patternSetGenerator = new PatternSetGenerator(configuration.getExecutionConfig(), testcase);
      includedPattern = patternSetGenerator.generatePatternSet(includedNodes, version);
      final HashSet<String> includedMethodPattern = new HashSet<>(includedPattern);
      testExecutor.setIncludedMethods(includedMethodPattern);
      return testExecutor;
   }

   @Override
   public boolean checkIsDecidable(final TestCase testcase, final int vmid) {
      try {
         getDurationsVersion(configuration.getExecutionConfig().getCommit());
         getDurationsVersion(configuration.getExecutionConfig().getCommitOld());
         boolean allDecidable = super.checkIsDecidable(testcase, vmid);
         LOG.debug("Super decidable: {}", allDecidable);
         for (final CallTreeNode includedNode : includedNodes) {
            allDecidable &= checkLevelDecidable(vmid, allDecidable, includedNode);
         }
         LOG.debug("Level decideable: {}", allDecidable);
         return allDecidable;
      } catch (ViewNotFoundException | AnalysisConfigurationException e) {
         throw new RuntimeException(e);
      }
   }

   private boolean checkLevelDecidable(final int vmid, final boolean allDecidable, final CallTreeNode includedNode) {
      final SummaryStatistics statisticsOld = includedNode.getStatistics(configuration.getExecutionConfig().getCommitOld());
      final SummaryStatistics statistics = includedNode.getStatistics(configuration.getExecutionConfig().getCommit());
      final EarlyBreakDecider decider = new EarlyBreakDecider(configuration, statisticsOld, statistics);
      final boolean nodeDecidable = decider.isBreakPossible(vmid);
      LOG.debug("{} decideable: {}", includedNode.getKiekerPattern(), allDecidable);
      LOG.debug("Old: {} {} Current: {} {}", statisticsOld.getMean(), statisticsOld.getStandardDeviation(),
            statistics.getMean(), statistics.getStandardDeviation());
      return nodeDecidable;
   }

   @Override
   public void handleKiekerResults(final String version, final File versionResultFolder) {
      if (getCurrentOrganizer().testSuccess(version)) {
         LOG.info("Did succeed in measurement - analyse values");

         boolean isOtherVersion = version.equals(configuration.getExecutionConfig().getCommit());
         final KiekerResultReader kiekerResultReader = new KiekerResultReader(configuration.getKiekerConfig().isUseAggregation(), configuration.getKiekerConfig().getRecord(),
               includedNodes, version, testcase,
               isOtherVersion);
         kiekerResultReader.setConsiderNodePosition(!configuration.getKiekerConfig().isUseAggregation());

         kiekerResultReader.readResults(versionResultFolder);
      } else {
         LOG.info("Did not success in measurement");
      }

   }

   public void setIncludedMethods(final Set<CallTreeNode> children) {
      includedNodes = children;
   }

   public void getDurations(final int levelId)
         throws FileNotFoundException, IOException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException {
      getDurationsVersion(configuration.getExecutionConfig().getCommit());
      getDurationsVersion(configuration.getExecutionConfig().getCommitOld());
   }

   public void cleanup(final int levelId) throws IOException {
      organizeMeasurements(levelId, configuration.getExecutionConfig().getCommit(), configuration.getExecutionConfig().getCommit());
      organizeMeasurements(levelId, configuration.getExecutionConfig().getCommit(), configuration.getExecutionConfig().getCommitOld());
   }

   private void organizeMeasurements(final int levelId, final String mainVersion, final String version) throws IOException {
      final File testcaseFolder = folders.getFullResultFolder(testcase, mainVersion, version);
      final File versionFolder = new File(folders.getArchiveResultFolder(mainVersion, testcase), version);
      if (!versionFolder.exists()) {
         versionFolder.mkdir();
      }
      final File adaptiveRunFolder = new File(versionFolder, "" + levelId);
      FileUtils.moveDirectory(testcaseFolder, adaptiveRunFolder);
   }

   private void getDurationsVersion(final String version) throws ViewNotFoundException, AnalysisConfigurationException {
      includedNodes.forEach(node -> node.createStatistics(version));
   }

   public void setCurrentVersion(final String version) {
      configuration.getExecutionConfig().setCommit(version);
      configuration.getExecutionConfig().setCommitOld(version + "~1");
   }

}
