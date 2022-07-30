package de.dagere.peass.measurement.rca.kieker;

import java.io.File;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.KiekerResultManager;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.traces.KiekerFolderUtil;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kiekerReading.KiekerDurationReader;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;

public class TreeReader extends KiekerResultManager {

   private boolean ignoreEOIs = true;
   private final MeasurementConfig realConfig;
   
   TreeReader(final PeassFolders folders, final MeasurementConfig config, final EnvironmentVariables env) {
      super(folders, config.getExecutionConfig(), config.getKiekerConfig(), env);
      testTransformer.getConfig().getKiekerConfig().setUseKieker(true);
      testTransformer.getConfig().getKiekerConfig().setUseAggregation(false);
      testTransformer.getConfig().getKiekerConfig().setRecord(AllowedKiekerRecord.OPERATIONEXECUTION);
      this.realConfig = config;
   }

   public void setIgnoreEOIs(final boolean ignoreEOIs) {
      this.ignoreEOIs = ignoreEOIs;
   }

   public CallTreeNode getTree(final TestCase testcase, final String commit) {
      executeMeasurements(testcase, commit);
      
      File resultsFolder = KiekerFolderUtil.getModuleResultFolder(folders, testcase);
      File kiekerTraceFolder = KiekerFolderUtil.getClazzMethodFolder(testcase, resultsFolder)[0];

      CallTreeNode root = readTree(testcase, kiekerTraceFolder);
      return root;
   }

   private CallTreeNode readTree(final TestCase testcase, final File kiekerTraceFolder) {
      final ModuleClassMapping mapping = new ModuleClassMapping(folders.getProjectFolder(), executor.getModules(), realConfig.getExecutionConfig());
      
      TreeStage stage = KiekerDurationReader.executeTreeStage(kiekerTraceFolder, testcase, ignoreEOIs, realConfig, mapping);

      CallTreeNode root = stage.getRoot();
      if (root == null) {
         throw new RuntimeException("An error occured - root node of " + testcase + " could not be identified!");
      }
      return root;
   }

   private void executeMeasurements(final TestCase testcase, final String commit) {
      executor.loadClasses();
      executeKoPeMeKiekerRun(new TestSet(testcase), commit, folders.getTreeLogFolder());
   }

}
