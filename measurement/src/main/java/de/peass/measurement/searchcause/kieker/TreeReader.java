package de.peass.measurement.searchcause.kieker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.KiekerResultManager;
import de.peass.dependency.analysis.KiekerReader;
import de.peass.dependency.analysis.ModuleClassMapping;
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

   public TreeReader(final File projectFolder, final long timeout) {
      super(projectFolder, timeout);
   }
   
   @Override
   public void executeKoPeMeKiekerRun(TestSet testsToUpdate, String version) throws IOException, XmlPullParserException, InterruptedException {
      executor.loadClasses();
      super.executeKoPeMeKiekerRun(testsToUpdate, version);
   }
   
   public CallTreeNode getTree(TestCase testcase)
         throws FileNotFoundException, IOException, XmlPullParserException, ViewNotFoundException, AnalysisConfigurationException {
      File resultsFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
      File kiekerTraceFolder = KiekerFolderUtil.getClazzMethodFolder(testcase, resultsFolder);
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

}
