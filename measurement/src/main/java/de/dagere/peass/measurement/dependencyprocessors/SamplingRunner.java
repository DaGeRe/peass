package de.dagere.peass.measurement.dependencyprocessors;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import de.dagere.peass.measurement.rca.data.CallTreeNode;
import io.github.terahidro2003.config.Config;
import io.github.terahidro2003.result.SamplerResultsProcessor;
import io.github.terahidro2003.samplers.SamplerExecutorPipeline;
import io.github.terahidro2003.samplers.asyncprofiler.AsyncProfilerExecutor;
import io.github.terahidro2003.samplers.asyncprofiler.MeasurementInformation;
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
   public void runOnce(TestMethodCall testcase, String commit, int vmid, File logFolder, File projectFolder) {
      initCommit(commit);

      final File vmidFolder = initVMFolder(commit, vmid, logFolder);
      
      testExecutor.prepareKoPeMeExecution(new File(logFolder, "clean.txt"));

      final long outerTimeout = 10 + (int) (this.testTransformer.getConfig().getTimeoutInSeconds() * 1.2);

      try {
         /*
         CONFIGURATION

         Most properties should be null, as SJSW executor itself won't be used
         (i.e., SJSW won't attach async-profiler's Java agent by itself)
         profilerPath is intentionally left null as SJSW it this case should download, extract and locate
         async-profiler's executable.
          */
         Config config = new Config(
                 null,
                 null,
                 null,
                 vmidFolder.getAbsolutePath() + "/sjsw-results",
                 true,
                 100
         );

         /*
            DURATION (TIMEOUT)

            Sampling duration is the equivalent to the timeout of the test.
            Sampling continues until either the test terminates by itself, or after the sampling duration.
          */
         Duration duration = Duration.ofSeconds(outerTimeout);

         /*
            JAVA AGENT RETRIEVAL

            Retrieves java agent as string to be attached to the maven lifecycle
          */
         SamplerExecutorPipeline pipeline = new AsyncProfilerExecutor();
         MeasurementInformation agent = pipeline.javaAgent(config, duration);

         /*
            EXECUTION

            Executes the test with maven executor (sampling should be done as asprof agent is passed to the
            MavenExecutor).

            It should skip the instrumentation, assuming that useKieker = false
          */
         testExecutor.executeTest(testcase, logFolder, outerTimeout, agent.javaAgentPath());

         SamplerResultsProcessor processor = new SamplerResultsProcessor();
         var parsedJFR = processor.extractSamplesFromJFR(new File(agent.rawOutputPath()), config);

         // CallTreeNode requires two commits that are not provided here
         // where to provide oldCommit hash????
         // If everything works, then root should represent sampling results from a single commit
         CallTreeNode root = processor.convertResultsToPeassTree(parsedJFR, commit, commit);
         System.out.println(root);
      } catch (IOException e) {
          throw new RuntimeException(e);
      }
      
      LOG.info("Organizing result paths");
      currentOrganizer.saveResultFiles(commit, vmid);

      cleanup();
   }
}
