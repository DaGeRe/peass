package de.dagere.peass.measurement.dependencyprocessors;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.organize.ResultOrganizer;
import de.dagere.peass.testtransformation.TestTransformer;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionControlSystem;

public class OnceRunner extends AbstractMeasurementProcessRunner {

   private static final Logger LOG = LogManager.getLogger(OnceRunner.class);

   protected final TestTransformer testTransformer;
   protected final TestExecutor testExecutor;

   protected final ResultOrganizer currentOrganizer;
   private final KiekerResultHandler resultHandler;

   public OnceRunner(final PeassFolders folders, final TestExecutor testExecutor, final ResultOrganizer currentOrganizer, final KiekerResultHandler resultHandler) {
      super(folders);
      this.testTransformer = testExecutor.getTestTransformer();
      this.testExecutor = testExecutor;
      this.currentOrganizer = currentOrganizer;
      this.resultHandler = resultHandler;
      
      try {
         FileUtils.cleanDirectory(folders.getTempDir());
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   public void runOnce(final TestMethodCall testcase, final String commit, final int vmid, final File logFolder) {
      initCommit(commit);

      final File vmidFolder = initVMFolder(commit, vmid, logFolder);

      if (testTransformer.getConfig().getKiekerConfig().isUseKieker()) {
         testExecutor.loadClasses();
      }
      testExecutor.prepareKoPeMeExecution(new File(logFolder, "clean.txt"));
      final long outerTimeout = 10 + (int) (this.testTransformer.getConfig().getTimeoutInSeconds() * 1.2);
      testExecutor.executeTest(testcase, vmidFolder, outerTimeout);

      if (testTransformer.getConfig().isDirectlyMeasureKieker()) {
         DirectKiekerMeasurementTransformer measurementTransformer = new DirectKiekerMeasurementTransformer(folders);
         measurementTransformer.transform(testcase);
      }

      LOG.debug("Handling Kieker results");
      resultHandler.handleKiekerResults(commit, currentOrganizer.getTempResultsFolder(commit));

      
      if (!testTransformer.getConfig().getKiekerConfig().isDisableKiekerKoPeMe()) {
         LOG.info("Organizing result paths");
         currentOrganizer.saveResultFiles(commit, vmid);
      } else {
         LOG.info("Not organizing result paths since the export should go to ExplorViz");
      }
      

      cleanup();
   }

   public ResultOrganizer getCurrentOrganizer() {
      return currentOrganizer;
   }
}
