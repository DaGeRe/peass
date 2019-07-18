package de.peass.measurement.searchcause.kieker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.KiekerResultManager;
import de.peass.dependency.analysis.KiekerReader;
import de.peass.dependency.analysis.PeASSFilter;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.traces.KiekerFolderUtil;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.searchcause.data.CallTreeNode;
import kieker.analysis.AnalysisController;
import kieker.analysis.exception.AnalysisConfigurationException;
import kieker.tools.traceAnalysis.filter.traceReconstruction.TraceReconstructionFilter;

public class TreeReader extends KiekerResultManager {

   TreeReader(final File folder, final int timeout) throws InterruptedException, IOException {
      super(folder, timeout);
   }

   @Override
   public void executeKoPeMeKiekerRun(TestSet testsToUpdate, String version) throws IOException, XmlPullParserException, InterruptedException {
      executor.loadClasses();
      super.executeKoPeMeKiekerRun(testsToUpdate, version);
   }
   
   public CallTreeNode getTree(TestCase testcase, String version)
         throws FileNotFoundException, IOException, XmlPullParserException, ViewNotFoundException, AnalysisConfigurationException, InterruptedException {
      executeMeasurements(testcase, version);
      
      File resultsFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
      File kiekerTraceFolder = KiekerFolderUtil.getClazzMethodFolder(testcase, resultsFolder);
      
      CallTreeNode root = readTree(testcase, kiekerTraceFolder);
      return root;
   }

   private CallTreeNode readTree(TestCase testcase, File kiekerTraceFolder) throws AnalysisConfigurationException {
      KiekerReader reader = new KiekerReader(kiekerTraceFolder);
      reader.initBasic();
      TraceReconstructionFilter traceReconstructionFilter = reader.initTraceReconstruction();
      
      AnalysisController analysisController = reader.getAnalysisController();
      final TreeFilter kopemeFilter = new TreeFilter(null, analysisController,  testcase);
      analysisController.connect(traceReconstructionFilter, TraceReconstructionFilter.OUTPUT_PORT_NAME_EXECUTION_TRACE,
            kopemeFilter, PeASSFilter.INPUT_EXECUTION_TRACE);

      analysisController.run();
      
      CallTreeNode root = kopemeFilter.getRoot();
      return root;
   }

   private void executeMeasurements(TestCase testcase, String version) throws IOException, XmlPullParserException, InterruptedException {
      getExecutor().loadClasses();
      executeKoPeMeKiekerRun(new TestSet(testcase), version);
   }

}
