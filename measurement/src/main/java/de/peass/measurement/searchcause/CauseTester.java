package de.peass.measurement.searchcause;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.KiekerReader;
import de.peass.dependency.analysis.PeASSFilter;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MavenTestExecutor;
import de.peass.dependency.traces.KiekerFolderUtil;
import de.peass.dependencyprocessors.AdaptiveTester;
import de.peass.dependencyprocessors.ResultOrganizer;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.searchcause.data.CallTreeNode;
import de.peass.measurement.searchcause.kieker.DurationFilter;
import de.peass.testtransformation.JUnitTestTransformer;
import kieker.analysis.AnalysisController;
import kieker.analysis.exception.AnalysisConfigurationException;
import kieker.tools.traceAnalysis.filter.traceReconstruction.TraceReconstructionFilter;

public class CauseTester extends AdaptiveTester {

   private static final Logger LOG = LogManager.getLogger(AdaptiveTester.class);

   private Set<CallTreeNode> includedMethods;
   private TestCase testcase;

   public CauseTester(PeASSFolders project, JUnitTestTransformer testgenerator, MeasurementConfiguration configuraiton, TestCase testcase) throws IOException {
      super(project, testgenerator, configuraiton);
      this.testcase = testcase;
      testgenerator.setUseKieker(true);
      testgenerator.setAdaptiveExecution(true);
   }

   @Override
   public void evaluate(String version, String versionOld, TestCase testcase) throws IOException, InterruptedException, JAXBException {
      LOG.debug("Adaptive execution: " + includedMethods);
//      prepareAdaptiveExecution();
      
      Set<String> includedPattern = new HashSet<>();
      includedMethods.forEach(node -> includedPattern.add(node.getKiekerPattern()));
      testExecutor.setIncludedMethods(includedPattern);
      super.evaluate(version, versionOld, testcase);
   }


   public void setIncludedMethods(Set<CallTreeNode> children) {
      includedMethods = children;
   }

   public void getDurations(String version, String versionOld)
         throws FileNotFoundException, IOException, XmlPullParserException, AnalysisConfigurationException, ViewNotFoundException {
      currentOrganizer = new ResultOrganizer(folders, version, currentChunkStart, testTransformer.isUseKieker());
      getDurationsVersion(version);
      getDurationsVersion(versionOld);
   }

   private void getDurationsVersion(String version) throws ViewNotFoundException, AnalysisConfigurationException {
      File versionResultFolder = currentOrganizer.getFullResultFolder(testcase, version);

      for (File kiekerResultFolder : versionResultFolder.listFiles((FilenameFilter) new RegexFileFilter("[0-9]*"))) {
         File kiekerTraceFile = KiekerFolderUtil.getKiekerTraceFolder(kiekerResultFolder, testcase);
         KiekerReader reader = new KiekerReader(kiekerTraceFile);
         reader.initBasic();
         TraceReconstructionFilter traceReconstructionFilter = reader.initTraceReconstruction();

         AnalysisController analysisController = reader.getAnalysisController();

         final DurationFilter kopemeFilter = new DurationFilter(includedMethods, analysisController, version);
         analysisController.connect(traceReconstructionFilter, TraceReconstructionFilter.OUTPUT_PORT_NAME_EXECUTION_TRACE,
               kopemeFilter, PeASSFilter.INPUT_EXECUTION_TRACE);

         analysisController.run();
      }
      includedMethods.forEach(node -> node.createStatistics(version, testTransformer.getIterations() / 2));
   }

   public static void main(String[] args) throws IOException, XmlPullParserException, InterruptedException, JAXBException {
      File projectFolder = new File("../../projekte/commons-fileupload");
      String version = "4ed6e923cb2033272fcb993978d69e325990a5aa";
      TestCase test = new TestCase("org.apache.commons.fileupload.ServletFileUploadTest", "testFoldedHeaders");

      MeasurementConfiguration config = new MeasurementConfiguration(15, 15, 0.01, 0.05);
      CauseTester manager = new CauseTester(new PeASSFolders(projectFolder), new JUnitTestTransformer(projectFolder), config, test);

      manager.evaluate(version, version + "~1", test);
   }
}
