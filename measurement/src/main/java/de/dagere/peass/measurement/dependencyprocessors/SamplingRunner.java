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
import de.dagere.peass.measurement.rca.searcher.ICauseSearcher;
import de.dagere.peass.testtransformation.TestTransformer;

public class SamplingRunner extends AbstractMeasurementProcessRunner {
   private static final Logger LOG = LogManager.getLogger(OnceRunner.class);

   protected final TestTransformer testTransformer;
   protected final TestExecutor testExecutor;

   protected final ResultOrganizer currentOrganizer;
   private final ICauseSearcher resultHandler;

   public SamplingRunner(final PeassFolders folders, final TestExecutor testExecutor, final ResultOrganizer currentOrganizer, final ICauseSearcher resultHandler) {
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

   @Override
   public void runOnce(TestMethodCall testcase, String commit, int vmid, File logFolder) {
      initCommit(commit);

      final File vmidFolder = initVMFolder(commit, vmid, logFolder);
      
      testExecutor.prepareKoPeMeExecution(new File(logFolder, "clean.txt"));
      
      //TODO implement sampling measurement
      if (true) {
         throw new RuntimeException("Not implemented yet");
      }
      
      LOG.info("Organizing result paths");
      currentOrganizer.saveResultFiles(commit, vmid);

      cleanup();
   }
}
