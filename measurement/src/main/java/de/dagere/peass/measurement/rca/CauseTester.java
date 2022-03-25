package de.dagere.peass.measurement.rca;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
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

   public CauseTester(final CauseSearchFolders project, final MeasurementConfig measurementConfig, final CauseSearcherConfig causeConfig, final EnvironmentVariables env)
         throws IOException {
      super(project, measurementConfig, env);
      this.testcase = causeConfig.getTestCase();
      this.causeConfig = causeConfig;
      this.folders = project;
   }

   public void measureVersion(final List<CallTreeNode> nodes)
         throws IOException, XmlPullParserException, InterruptedException, ViewNotFoundException, AnalysisConfigurationException, JAXBException {
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
   public void evaluate(final TestCase testcase) throws IOException, InterruptedException, JAXBException, XmlPullParserException {
      LOG.debug("Adaptive execution: " + includedNodes);
      
      initEvaluation(testcase);

      final File logFolder = folders.getRCALogFolder(configuration.getExecutionConfig().getVersion(), testcase, levelId);
      
      try (ProgressWriter writer = new ProgressWriter(folders.getProgressFile(), configuration.getVms())){
         evaluateWithAdaption(testcase, logFolder, writer);
      }
   }

   @Override
   protected synchronized TestExecutor getExecutor(final PeassFolders temporaryFolders, final String version) {
      final TestExecutor testExecutor = super.getExecutor(temporaryFolders, version);
      TestTransformer testTransformer = testExecutor.getTestTransformer();
      testTransformer.setIgnoreEOIs(causeConfig.isIgnoreEOIs());
      generatePatternSet(version);
      final HashSet<String> includedMethodPattern = new HashSet<>(includedPattern);
      testExecutor.setIncludedMethods(includedMethodPattern);
      return testExecutor;
   }

   private void generatePatternSet(final String version) {
      includedPattern = new HashSet<>();
      if (configuration.getExecutionConfig().getVersionOld().equals(version)) {
         includedNodes.forEach(node -> {
            LOG.trace(node);
            if (!node.getKiekerPattern().equals(CauseSearchData.ADDED)) {
               includedPattern.add(node.getKiekerPattern());
            }

         });
      } else {
         LOG.debug("Searching other: " + version);
         includedNodes.forEach(node -> {
            LOG.trace(node);
            if (!node.getOtherKiekerPattern().equals(CauseSearchData.ADDED)) {
               includedPattern.add(node.getOtherKiekerPattern());
            }
         });
      }
   }

   @Override
   public boolean checkIsDecidable(final TestCase testcase, final int vmid) throws JAXBException {
      try {
         getDurationsVersion(configuration.getExecutionConfig().getVersion());
         getDurationsVersion(configuration.getExecutionConfig().getVersionOld());
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

   private boolean checkLevelDecidable(final int vmid, final boolean allDecidable, final CallTreeNode includedNode) throws JAXBException {
      final SummaryStatistics statisticsOld = includedNode.getStatistics(configuration.getExecutionConfig().getVersionOld());
      final SummaryStatistics statistics = includedNode.getStatistics(configuration.getExecutionConfig().getVersion());
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
         
         boolean isOtherVersion = version.equals(configuration.getExecutionConfig().getVersion());
         final KiekerResultReader kiekerResultReader = new KiekerResultReader(configuration.getKiekerConfig().isUseAggregation(), configuration.getKiekerConfig().getRecord(), includedNodes, version, testcase,
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
      getDurationsVersion(configuration.getExecutionConfig().getVersion());
      getDurationsVersion(configuration.getExecutionConfig().getVersionOld());
   }

   public void cleanup(final int levelId) throws IOException {
      organizeMeasurements(levelId, configuration.getExecutionConfig().getVersion(), configuration.getExecutionConfig().getVersion());
      organizeMeasurements(levelId, configuration.getExecutionConfig().getVersion(), configuration.getExecutionConfig().getVersionOld());
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

   public static void main(final String[] args) throws IOException, XmlPullParserException, InterruptedException, JAXBException, ClassNotFoundException {
      final File projectFolder = new File("../../projekte/commons-fileupload");
      final String version = "4ed6e923cb2033272fcb993978d69e325990a5aa";
      final TestCase test = new TestCase("org.apache.commons.fileupload.ServletFileUploadTest", "testFoldedHeaders");

      final MeasurementConfig config = new MeasurementConfig(15 * 1000 * 60, 15, true, version, version + "~1");
      config.setUseKieker(true);
      final CauseSearcherConfig causeConfig = new CauseSearcherConfig(test, false, 0.01, false, false, RCAStrategy.COMPLETE, 1);
      final CauseTester manager = new CauseTester(new CauseSearchFolders(projectFolder), config, causeConfig, new EnvironmentVariables(config.getExecutionConfig().getProperties()));

      final CallTreeNode node = new CallTreeNode("FileUploadTestCase#parseUpload",
            "protected java.util.List org.apache.commons.fileupload.FileUploadTestCase.parseUpload(byte[],java.lang.String)",
            "protected java.util.List org.apache.commons.fileupload.FileUploadTestCase.parseUpload(byte[],java.lang.String)",
            config);
      node.setOtherVersionNode(node);
      final Set<CallTreeNode> nodes = new HashSet<>();
      nodes.add(node);
      manager.setIncludedMethods(nodes);
      manager.runOnce(test, version, 0, new File("log"));
      // manager.evaluate(test);
   }

   public void setCurrentVersion(final String version) {
      configuration.getExecutionConfig().setVersion(version);
      configuration.getExecutionConfig().setVersionOld(version + "~1");
   }

}
