package de.dagere.peass.measurement.dependencyprocessors;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.organize.ResultOrganizer;
import de.dagere.peass.measurement.rca.searcher.ICauseSearcher;
import de.dagere.peass.testtransformation.TestTransformer;
import io.github.terahidro2003.config.Config;
import io.github.terahidro2003.samplers.SamplerExecutorPipeline;
import io.github.terahidro2003.samplers.asyncprofiler.AsyncProfilerExecutor;
import io.github.terahidro2003.samplers.asyncprofiler.MeasurementInformation;

public class SamplingRunner extends AbstractMeasurementProcessRunner {
   private static final Logger LOG = LogManager.getLogger(OnceRunner.class);

   protected final TestTransformer testTransformer;
   protected final TestExecutor testExecutor;

   protected final ResultOrganizer currentOrganizer;
   private final ICauseSearcher resultHandler;

   private final Config configuration;

   public SamplingRunner(final PeassFolders folders, final TestExecutor testExecutor, final ResultOrganizer currentOrganizer, final ICauseSearcher resultHandler, final Config config) {
      super(folders);
      this.testTransformer = testExecutor.getTestTransformer();
      this.testExecutor = testExecutor;
      this.currentOrganizer = currentOrganizer;
      this.resultHandler = resultHandler;
      this.configuration = config;
      
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
      
      Duration duration = Duration.ofSeconds(300);
      SamplerExecutorPipeline pipeline = new AsyncProfilerExecutor();
      MeasurementInformation agent = pipeline.javaAgent(this.configuration, vmid, commit, duration);
      
      testExecutor.executeTest(agent.javaAgentPath(), testcase, vmidFolder, vmid);
      
      LOG.info("Organizing result paths");
      currentOrganizer.saveResultFiles(commit, vmid);

      cleanup();
   }
}
