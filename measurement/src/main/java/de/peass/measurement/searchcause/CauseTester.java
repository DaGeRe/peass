package de.peass.measurement.searchcause;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.KiekerReader;
import de.peass.dependency.analysis.PeASSFilter;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.traces.KiekerFolderUtil;
import de.peass.dependencyprocessors.AdaptiveTester;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.analysis.EarlyBreakDecider;
import de.peass.measurement.organize.ResultOrganizer;
import de.peass.measurement.searchcause.data.CallTreeNode;
import de.peass.measurement.searchcause.kieker.DurationFilter;
import de.peass.testtransformation.JUnitTestTransformer;
import kieker.analysis.AnalysisController;
import kieker.analysis.exception.AnalysisConfigurationException;
import kieker.tools.traceAnalysis.filter.executionRecordTransformation.ExecutionRecordTransformationFilter;

/**
 * Measures method calls adaptively instrumented by Kieker
 * 
 * @author reichelt
 *
 */
public class CauseTester extends AdaptiveTester {

   private static final Logger LOG = LogManager.getLogger(AdaptiveTester.class);

   private Set<CallTreeNode> includedNodes;
   private final TestCase testcase;

   public CauseTester(final PeASSFolders project, final JUnitTestTransformer testgenerator, final MeasurementConfiguration configuraiton, final TestCase testcase)
         throws IOException {
      super(project, testgenerator, configuraiton);
      this.testcase = testcase;
      testgenerator.setUseKieker(true);
      testgenerator.setAdaptiveExecution(true);
   }

   @Override
   public void evaluate(final String version, final String versionOld, final TestCase testcase) throws IOException, InterruptedException, JAXBException {
      includedNodes.forEach(node -> node.setWarmup(testTransformer.getIterations() / 2));

      LOG.debug("Adaptive execution: " + includedNodes);
      // prepareAdaptiveExecution();

      final Set<String> includedPattern = new HashSet<>();
      includedNodes.forEach(node -> includedPattern.add(node.getKiekerPattern()));
      testExecutor.setIncludedMethods(includedPattern);
      super.evaluate(version, versionOld, testcase);
   }

   @Override
   protected boolean checkIsDecidable(final String version, final String versionOld, final TestCase testcase, final int vmid) throws JAXBException {
      try {
         getDurationsVersion(version);
         getDurationsVersion(versionOld);
         boolean allDecidable = super.checkIsDecidable(version, versionOld, testcase, vmid);
         for (final CallTreeNode includedNode : includedNodes) {
            final SummaryStatistics statisticsOld = includedNode.getStatistics(versionOld);
            final SummaryStatistics statistics = includedNode.getStatistics(version);
            final EarlyBreakDecider decider = new EarlyBreakDecider(testTransformer, statisticsOld, statistics, version, versionOld, testcase, vmid);
            allDecidable &= decider.isBreakPossible(vmid);
         }
         return allDecidable;
      } catch (ViewNotFoundException | AnalysisConfigurationException e) {
         throw new RuntimeException(e);
      }
   }

   @Override
   protected void handleKiekerResults(final String version, final File versionResultFolder) {
      try {
         for (final File kiekerResultFolder : versionResultFolder.listFiles((FilenameFilter) new RegexFileFilter("[0-9]*"))) {
            final File kiekerTraceFile = KiekerFolderUtil.getKiekerTraceFolder(kiekerResultFolder, testcase);
            final KiekerReader reader = new KiekerReader(kiekerTraceFile);

            reader.initBasic();
            final AnalysisController analysisController = reader.getAnalysisController();

            final DurationFilter kopemeFilter = new DurationFilter(includedNodes, analysisController, version);
            analysisController.connect(reader.getExecutionFilter(), ExecutionRecordTransformationFilter.OUTPUT_PORT_NAME_EXECUTIONS,
                  kopemeFilter, PeASSFilter.INPUT_EXECUTION_TRACE);

            analysisController.run();
         }
      } catch (IllegalStateException | AnalysisConfigurationException e) {
         e.printStackTrace();
      }
   }

   public void setIncludedMethods(final Set<CallTreeNode> children) {
      includedNodes = children;
   }

   public void getDurations(final String version, final String versionOld, final int adaptiveId)
         throws FileNotFoundException, IOException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException {
      currentOrganizer = new ResultOrganizer(folders, version, currentChunkStart, testTransformer.isUseKieker());
      getDurationsVersion(version);
      getDurationsVersion(versionOld);

      organizeMeasurements(adaptiveId, version);
   }

   private void organizeMeasurements(final int adaptiveId, final String version) {
      final File testcaseFolder = new File(folders.getDetailResultFolder(), testcase.getClazz());
      final File adaptiveRunFolder = new File(folders.getDetailResultFolder(version, testcase), "" + adaptiveId);
      testcaseFolder.renameTo(adaptiveRunFolder);
   }

   private void getDurationsVersion(final String version) throws ViewNotFoundException, AnalysisConfigurationException {
      includedNodes.forEach(node -> node.createStatistics(version));
   }

   public static void main(final String[] args) throws IOException, XmlPullParserException, InterruptedException, JAXBException {
      final File projectFolder = new File("../../projekte/commons-fileupload");
      final String version = "4ed6e923cb2033272fcb993978d69e325990a5aa";
      final TestCase test = new TestCase("org.apache.commons.fileupload.ServletFileUploadTest", "testFoldedHeaders");

      final MeasurementConfiguration config = new MeasurementConfiguration(15, 15, 0.01, 0.05);
      final CauseTester manager = new CauseTester(new PeASSFolders(projectFolder), new JUnitTestTransformer(projectFolder), config, test);

      manager.evaluate(version, version + "~1", test);
   }
}
