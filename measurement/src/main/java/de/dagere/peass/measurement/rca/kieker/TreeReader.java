package de.dagere.peass.measurement.rca.kieker;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.KiekerResultManager;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.traces.KiekerFolderUtil;
import de.dagere.peass.dependencyprocessors.ViewNotFoundException;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kiekerReading.KiekerDurationReader;
import kieker.analysis.exception.AnalysisConfigurationException;

public class TreeReader extends KiekerResultManager {

   private boolean ignoreEOIs = true;
   private final MeasurementConfiguration config;
   
   TreeReader(final PeassFolders folders, final MeasurementConfiguration config, final EnvironmentVariables env) throws InterruptedException, IOException {
      super(folders, config.getExecutionConfig(), config.getKiekerConfig(), env);
      this.config = config;
   }

   public void setIgnoreEOIs(final boolean ignoreEOIs) {
      this.ignoreEOIs = ignoreEOIs;
   }

   public CallTreeNode getTree(final TestCase testcase, final String version)
         throws FileNotFoundException, IOException, XmlPullParserException, ViewNotFoundException, AnalysisConfigurationException, InterruptedException {
      executeMeasurements(testcase, version);
      
      File resultsFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
      File kiekerTraceFolder = KiekerFolderUtil.getClazzMethodFolder(testcase, resultsFolder)[0];

      CallTreeNode root = readTree(testcase, kiekerTraceFolder);
      return root;
   }

   private CallTreeNode readTree(final TestCase testcase, final File kiekerTraceFolder) throws AnalysisConfigurationException, IOException, XmlPullParserException {
      final ModuleClassMapping mapping = new ModuleClassMapping(folders.getProjectFolder(), executor.getModules());
      
      TreeStage stage = KiekerDurationReader.executeDurationStage(kiekerTraceFolder, testcase, ignoreEOIs, config, mapping);

      CallTreeNode root = stage.getRoot();
      if (root == null) {
         throw new RuntimeException("An error occured - root node of " + testcase + " could not be identified!");
      }
      return root;
   }

   private void executeMeasurements(final TestCase testcase, final String version) throws IOException, XmlPullParserException, InterruptedException {
      executor.loadClasses();
      executeKoPeMeKiekerRun(new TestSet(testcase), version, folders.getTreeLogFolder());
   }

}
