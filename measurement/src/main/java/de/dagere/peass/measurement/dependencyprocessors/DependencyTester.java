package de.dagere.peass.measurement.dependencyprocessors;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.config.FixedCommitConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.config.MeasurementStrategy;
import de.dagere.peass.dependency.ExecutorCreator;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.execution.processutils.ProcessBuilderHelper;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.cleaning.Cleaner;
import de.dagere.peass.measurement.dataloading.DataReader;
import de.dagere.peass.measurement.dependencyprocessors.helper.ProgressWriter;
import de.dagere.peass.measurement.dependencyprocessors.reductioninfos.ReductionManager;
import de.dagere.peass.measurement.organize.FolderDeterminer;
import de.dagere.peass.measurement.organize.ResultOrganizer;
import de.dagere.peass.measurement.organize.ResultOrganizerParallel;
import de.dagere.peass.measurement.statistics.data.TestData;
import de.dagere.peass.testtransformation.TestTransformer;

/**
 * Runs a PeASS with only running the tests where a changed class is present.
 * 
 * @author reichelt
 *
 */
public class DependencyTester implements KiekerResultHandler {

   private static final Logger LOG = LogManager.getLogger(DependencyTester.class);

   protected final PeassFolders folders;
   protected final MeasurementConfig configuration;
   protected final EnvironmentVariables env;
   private ResultOrganizer currentOrganizer;
   protected long currentChunkStart = 0;
   private final CommitComparatorInstance comparator;
   protected final ReductionManager reductionManager; 

   public DependencyTester(final PeassFolders folders, final MeasurementConfig measurementConfig, final EnvironmentVariables env, CommitComparatorInstance comparator) {
      this.folders = folders;
      this.configuration = measurementConfig;
      this.env = env;
      this.comparator = comparator;
      reductionManager = new ReductionManager(measurementConfig);
   }

   /**
    * Compares the given testcase for the given commits.
    * 
    * @param commit Current commit to test
    * @param commitOld Old commit to test
    * @param testcase Testcase to test
    * @throws XmlPullParserException
    */
   public void evaluate(final TestMethodCall testcase) throws IOException, InterruptedException, XmlPullParserException {
      initEvaluation(testcase);

      final File logFolder = folders.getMeasureLogFolder(configuration.getFixedCommitConfig().getCommit(), testcase);
      try (ProgressWriter writer = new ProgressWriter(folders.getProgressFile(), configuration.getVms())) {
         evaluateSimple(testcase, logFolder, writer);
      }
   }

   protected void initEvaluation(final TestMethodCall testcase) {
      FixedCommitConfig fixedCommitConfig = configuration.getFixedCommitConfig();
      LOG.info("Executing test " + testcase.getClazz() + " " + testcase.getMethod() + " in commits {} and {}", fixedCommitConfig.getCommitOld(),
            fixedCommitConfig.getCommit());
      new FolderDeterminer(folders).testResultFolders(fixedCommitConfig.getCommit(), fixedCommitConfig.getCommitOld(), testcase);
   }

   private void evaluateSimple(final TestMethodCall testcase, final File logFolder, final ProgressWriter writer)
         throws IOException {
      currentChunkStart = System.currentTimeMillis();
      for (int finishedVMs = 0; finishedVMs < configuration.getVms(); finishedVMs++) {
         long comparisonStart = System.currentTimeMillis();

         runOneComparison(logFolder, testcase, finishedVMs);

         final boolean shouldBreak = reductionManager.updateExecutions(testcase, finishedVMs, getCurrentOrganizer());
         if (shouldBreak) {
            LOG.debug("Too few executions possible - finishing testing.");
            break;
         }

         long durationInSeconds = (System.currentTimeMillis() - comparisonStart) / 1000;
         writer.write(durationInSeconds, finishedVMs);

         betweenVMCooldown();
      }
   }

   protected void betweenVMCooldown() {
      if (configuration.isCallSyncBetweenVMs()) {
         ProcessBuilderHelper.syncToHdd();
      }
      try {
         Thread.sleep(configuration.getWaitTimeBetweenVMs());
      } catch (InterruptedException e) {
         throw new RuntimeException(e);
      }
   }

   public void postEvaluate() {
      if (currentOrganizer != null) {
         final File cleanFolder = folders.getNativeCleanFolder();
         if (!cleanFolder.exists()) {
            cleanFolder.mkdirs();
         }
         final Cleaner cleaner = new Cleaner(cleanFolder, comparator);
         for (final File clazzFile : folders.getDetailResultFolder().listFiles()) {
            final Map<String, TestData> testdata = DataReader.readClassFolder(clazzFile, comparator);
            for (final Map.Entry<String, TestData> entry : testdata.entrySet()) {
               cleaner.processTestdata(entry.getValue());
            }
         }
      }
   }

   public void runOneComparison(final File logFolder, final TestMethodCall testcase, final int vmid) throws IOException {
      String[] commits = getVersions();

      if (configuration.getMeasurementStrategy().equals(MeasurementStrategy.SEQUENTIAL)) {
         LOG.info("Running sequential");
         runSequential(logFolder, testcase, vmid, commits);
      } else if (configuration.getMeasurementStrategy().equals(MeasurementStrategy.PARALLEL)) {
         LOG.info("Running parallel");
         runParallel(logFolder, testcase, vmid, commits);
      }
   }

   private String[] getVersions() {
      String commits[] = new String[2];
      commits[0] = configuration.getFixedCommitConfig().getCommitOld().equals("HEAD~1") ? configuration.getFixedCommitConfig().getCommit() + "~1"
            : configuration.getFixedCommitConfig().getCommitOld();
      commits[1] = configuration.getFixedCommitConfig().getCommit();
      return commits;
   }

   private void runParallel(final File logFolder, final TestMethodCall testcase, final int vmid, final String[] commits) throws IOException {
      final ResultOrganizerParallel organizer = new ResultOrganizerParallel(folders, configuration.getFixedCommitConfig().getCommit(), currentChunkStart,
            configuration.getKiekerConfig().isUseKieker(),
            configuration.isSaveAll(), testcase,
            configuration.getAllIterations());
      currentOrganizer = organizer;
      final ParallelExecutionRunnable[] runnables = new ParallelExecutionRunnable[2];
      for (int i = 0; i < 2; i++) {
         final String commit = commits[i];
         runnables[i] = new ParallelExecutionRunnable(organizer, commit, testcase, vmid, logFolder, this, configuration.getExecutionConfig().getGitCryptKey());
      }
      runParallel(runnables);
   }

   public void runParallel(final ParallelExecutionRunnable[] runnables) {
      Thread[] threads = new Thread[2];
      for (int i = 0; i < 2; i++) {
         threads[i] = new Thread(runnables[i]);
         threads[i].start();
      }
      try {
         for (int i = 0; i < 2; i++) {
            threads[i].join();
         }
      } catch (InterruptedException e) {
         throw new RuntimeException(e);
      }
   }

   private void runSequential(final File logFolder, final TestMethodCall testcase, final int vmid, final String commits[])
         throws IOException {
      currentOrganizer = new ResultOrganizer(folders, configuration.getFixedCommitConfig().getCommit(), currentChunkStart, configuration.getKiekerConfig().isUseKieker(),
            configuration.isSaveAll(),
            testcase, configuration.getAllIterations());
      for (String commit : commits) {
         runOnce(testcase, commit, vmid, logFolder);
      }
   }

   public void runOnce(final TestMethodCall testcase, final String commit, final int vmid, final File logFolder) {
      final TestExecutor testExecutor = getExecutor(folders, commit);
      final OnceRunner runner = new OnceRunner(folders, testExecutor, getCurrentOrganizer(), this);
      runner.runOnce(testcase, commit, vmid, logFolder);
   }

   protected synchronized TestExecutor getExecutor(final PeassFolders currentFolders, final String commit) {
      TestTransformer transformer = ExecutorCreator.createTestTransformer(currentFolders, configuration.getExecutionConfig(), configuration);
      final TestExecutor testExecutor = ExecutorCreator.createExecutor(currentFolders, transformer, env);
      return testExecutor;
   }

   /**
    * This method can be overriden in order to handle kieker results before they are compressed
    * 
    * @param folder
    */
   @Override
   public void handleKiekerResults(final String commit, final File folder) {

   }

   public void setVersions(final String commit, final String commitOld) {
      configuration.getFixedCommitConfig().setCommit(commit);
      configuration.getFixedCommitConfig().setCommitOld(commitOld);
   }

   protected boolean checkIsDecidable(final TestMethodCall testcase, final int vmid) {
      return false;
   }

   public ResultOrganizer getCurrentOrganizer() {
      return currentOrganizer;
   }

   public PeassFolders getFolders() {
      return folders;
   }
   
   public ReductionManager getReductionManager() {
      return reductionManager;
   }
}
