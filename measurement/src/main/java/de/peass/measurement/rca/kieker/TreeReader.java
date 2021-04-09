package de.peass.measurement.rca.kieker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.KiekerResultManager;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.ModuleClassMapping;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependency.traces.KiekerFolderUtil;
import de.peass.dependencyprocessors.ViewNotFoundException;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.kiekerReading.KiekerDurationReader;
import kieker.analysis.exception.AnalysisConfigurationException;

public class TreeReader extends KiekerResultManager {

   private boolean ignoreEOIs = true;
   private final MeasurementConfiguration config;
   
   TreeReader(final PeASSFolders folders, final MeasurementConfiguration config, final EnvironmentVariables env) throws InterruptedException, IOException {
      super(folders, config.getExecutionConfig(), env);
      this.config = config;
   }

   public void setIgnoreEOIs(final boolean ignoreEOIs) {
      this.ignoreEOIs = ignoreEOIs;
   }

   @Override
   public void executeKoPeMeKiekerRun(final TestSet testsToUpdate, final String version) throws IOException, XmlPullParserException, InterruptedException {
      executor.loadClasses();
      super.executeKoPeMeKiekerRun(testsToUpdate, version);
   }

   public CallTreeNode getTree(final TestCase testcase, final String version)
         throws FileNotFoundException, IOException, XmlPullParserException, ViewNotFoundException, AnalysisConfigurationException, InterruptedException {
      executeMeasurements(testcase, version);
      
      File resultsFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
      File kiekerTraceFolder = KiekerFolderUtil.getClazzMethodFolder(testcase, resultsFolder);

      CallTreeNode root = readTree(testcase, kiekerTraceFolder);
      return root;
   }

   private CallTreeNode readTree(final TestCase testcase, final File kiekerTraceFolder) throws AnalysisConfigurationException, IOException, XmlPullParserException {
      final ModuleClassMapping mapping = new ModuleClassMapping(folders.getProjectFolder());
      
      TreeStage stage = KiekerDurationReader.executeDurationStage(kiekerTraceFolder, testcase, ignoreEOIs, config, mapping);

      CallTreeNode root = stage.getRoot();
      if (root == null) {
         throw new RuntimeException("An error occured - root node of " + testcase + " could not be identified!");
      }
      return root;
   }

   private void executeMeasurements(final TestCase testcase, final String version) throws IOException, XmlPullParserException, InterruptedException {
      executeKoPeMeKiekerRun(new TestSet(testcase), version);
   }

}
