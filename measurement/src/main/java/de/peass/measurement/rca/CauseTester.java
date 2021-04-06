package de.peass.measurement.rca;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependency.execution.TestExecutor;
import de.peass.dependencyprocessors.AdaptiveTester;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.analysis.EarlyBreakDecider;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.testtransformation.JUnitTestTransformer;
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

   public CauseTester(final CauseSearchFolders project, final MeasurementConfiguration measurementConfig, final CauseSearcherConfig causeConfig, final EnvironmentVariables env)
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
      nodes.forEach(node -> node.setVersions(configuration.getVersion(), configuration.getVersionOld()));
      return includedNodes;
   }

   @Override
   public void evaluate(final TestCase testcase) throws IOException, InterruptedException, JAXBException, XmlPullParserException {
      LOG.debug("Adaptive execution: " + includedNodes);
      super.evaluate(testcase);
   }

   @Override
   protected synchronized TestExecutor getExecutor(final PeASSFolders temporaryFolders, final String version) {
      final TestExecutor testExecutor = super.getExecutor(temporaryFolders, version);
      JUnitTestTransformer testTransformer = testExecutor.getTestTransformer();
      testTransformer.setAggregatedWriter(causeConfig.isUseAggregation());
      testTransformer.setIgnoreEOIs(causeConfig.isIgnoreEOIs());
      generatePatternSet(version);
      final HashSet<String> includedMethodPattern = new HashSet<>(includedPattern);
      testExecutor.setIncludedMethods(includedMethodPattern);
      return testExecutor;
   }

   private void generatePatternSet(final String version) {
      includedPattern = new HashSet<>();
      if (configuration.getVersionOld().equals(version)) {
         includedNodes.forEach(node -> {
            LOG.trace(node);
            if (!node.getKiekerPattern().equals("ADDED")) {
               includedPattern.add(node.getKiekerPattern());
            }

         });
      } else {
         LOG.debug("Searching other: " + version);
         includedNodes.forEach(node -> {
            LOG.trace(node);
            if (!node.getOtherVersionNode().getKiekerPattern().equals("ADDED")) {
               includedPattern.add(node.getOtherVersionNode().getKiekerPattern());
            }
         });
      }
   }

   @Override
   public boolean checkIsDecidable(final TestCase testcase, final int vmid) throws JAXBException {
      try {
         getDurationsVersion(configuration.getVersion());
         getDurationsVersion(configuration.getVersionOld());
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
      final SummaryStatistics statisticsOld = includedNode.getStatistics(configuration.getVersionOld());
      final SummaryStatistics statistics = includedNode.getStatistics(configuration.getVersion());
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
         final KiekerResultReader kiekerResultReader = new KiekerResultReader(causeConfig.isUseAggregation(), includedNodes, version, testcase,
               version.equals(configuration.getVersion()));
         kiekerResultReader.setConsiderNodePosition(!causeConfig.isUseAggregation());
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
      getDurationsVersion(configuration.getVersion());
      getDurationsVersion(configuration.getVersionOld());
   }

   public void cleanup(final int levelId) {
      organizeMeasurements(levelId, configuration.getVersion(), configuration.getVersion());
      organizeMeasurements(levelId, configuration.getVersion(), configuration.getVersionOld());
   }

   private void organizeMeasurements(final int levelId, final String mainVersion, final String version) {
      final File testcaseFolder = folders.getFullResultFolder(testcase, mainVersion, version);
      final File versionFolder = new File(folders.getArchiveResultFolder(mainVersion, testcase), version);
      if (!versionFolder.exists()) {
         versionFolder.mkdir();
      }
      final File adaptiveRunFolder = new File(versionFolder, "" + levelId);
      if (!testcaseFolder.renameTo(adaptiveRunFolder)) {
         LOG.error("Could not rename {}", testcaseFolder);
      }
   }

   private void getDurationsVersion(final String version) throws ViewNotFoundException, AnalysisConfigurationException {
      includedNodes.forEach(node -> node.createStatistics(version));
   }

   public static void main(final String[] args) throws IOException, XmlPullParserException, InterruptedException, JAXBException {
      final File projectFolder = new File("../../projekte/commons-fileupload");
      final String version = "4ed6e923cb2033272fcb993978d69e325990a5aa";
      final TestCase test = new TestCase("org.apache.commons.fileupload.ServletFileUploadTest", "testFoldedHeaders");

      final MeasurementConfiguration config = new MeasurementConfiguration(15 * 1000 * 60, 15, true, version, version + "~1");
      config.setUseKieker(true);
      final CauseSearcherConfig causeConfig = new CauseSearcherConfig(test, false, false, 0.01, false, false, RCAStrategy.COMPLETE);
      final CauseTester manager = new CauseTester(new CauseSearchFolders(projectFolder), config, causeConfig, new EnvironmentVariables());

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
      configuration.setVersion(version);
      configuration.setVersionOld(version + "~1");
   }

}
