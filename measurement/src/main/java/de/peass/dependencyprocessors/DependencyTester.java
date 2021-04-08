package de.peass.dependencyprocessors;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.peass.config.MeasurementConfiguration;
import de.peass.config.MeasurementStrategy;
import de.peass.dependency.ExecutorCreator;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependency.execution.TestExecutor;
import de.peass.dependency.traces.TemporaryProjectFolderUtil;
import de.peass.measurement.analysis.Cleaner;
import de.peass.measurement.analysis.DataReader;
import de.peass.measurement.analysis.statistics.TestData;
import de.peass.measurement.organize.FolderDeterminer;
import de.peass.measurement.organize.ResultOrganizer;
import de.peass.measurement.organize.ResultOrganizerParallel;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.vcs.VersionControlSystem;

/**
 * Runs a PeASS with only running the tests where a changed class is present.
 * 
 * @author reichelt
 *
 */
public class DependencyTester implements KiekerResultHandler {

   private static final Logger LOG = LogManager.getLogger(DependencyTester.class);

   protected final PeASSFolders folders;
   protected final MeasurementConfiguration configuration;
   protected final EnvironmentVariables env;
   private final VersionControlSystem vcs;
   private ResultOrganizer currentOrganizer;
   protected long currentChunkStart = 0;

   public DependencyTester(final PeASSFolders folders, final MeasurementConfiguration measurementConfig, final EnvironmentVariables env) throws IOException {
      this.folders = folders;
      this.configuration = measurementConfig;
      this.env = env;

      vcs = VersionControlSystem.getVersionControlSystem(folders.getProjectFolder());

   }

   /**
    * Compares the given testcase for the given versions.
    * 
    * @param version Current version to test
    * @param versionOld Old version to test
    * @param testcase Testcase to test
    * @throws XmlPullParserException
    */
   public void evaluate(final TestCase testcase) throws IOException, InterruptedException, JAXBException, XmlPullParserException {
      new FolderDeterminer(folders).testResultFolders(configuration.getVersion(), configuration.getVersionOld(), testcase);

      LOG.info("Executing test " + testcase.getClazz() + " " + testcase.getMethod() + " in versions {} and {}", configuration.getVersionOld(), configuration.getVersion());

      final File logFolder = getLogFolder(configuration.getVersion(), testcase);

      currentChunkStart = System.currentTimeMillis();
      for (int finishedVMs = 0; finishedVMs < configuration.getVms(); finishedVMs++) {
         runOneComparison(logFolder, testcase, finishedVMs);

         final boolean shouldBreak = updateExecutions(testcase, finishedVMs);
         if (shouldBreak) {
            LOG.debug("Too less executions possible - finishing testing.");
            break;
         }
      }
   }

   boolean updateExecutions(final TestCase testcase, final int vmid) throws JAXBException {
      boolean shouldBreak = false;
      final Result versionOldResult = getLastResult(configuration.getVersionOld(), testcase, vmid);
      final Result versionNewResult = getLastResult(configuration.getVersion(), testcase, vmid);
      if (vmid < 40) {
         int reducedIterations = Math.min(shouldReduce(configuration.getVersionOld(), versionOldResult),
               shouldReduce(configuration.getVersion(), versionNewResult));
         if (reducedIterations != configuration.getIterations()) {
            LOG.error("Should originally run {} iterations, but did not succeed - reducing to {}", configuration.getIterations(), reducedIterations);
            // final int lessIterations = testTransformer.getConfig().getIterations() / 5;
            shouldBreak = reduceExecutions(shouldBreak, reducedIterations);
         }
      }

      return shouldBreak;
   }

   private int shouldReduce(final String version, final Result result) {
      final int reducedIterations;
      if (result == null) {
         reducedIterations = configuration.getIterations() / 2;
         LOG.error("Measurement for {} is null", version);
      } else if (result.getIterations() < configuration.getIterations()) {
         LOG.error("Measurement executions: {}", result.getIterations());
         final int minOfExecuted = (int) result.getIterations() - 2;
         reducedIterations = Math.min(minOfExecuted, configuration.getIterations() / 2);
         // 10E7 for at least 10 iterations means more than ~2.5 minutes per VM, which is ok
         // } else if (result.getValue() > 10E7 && testTransformer.getConfig().getIterations() > 10) {
         // reducedIterations = testTransformer.getConfig().getIterations() / 2;
      } else {
         reducedIterations = configuration.getIterations();
      }
      return reducedIterations;
   }

   protected boolean reduceExecutions(boolean shouldBreak, final int lessIterations) {
      if (lessIterations > 3) {
         LOG.info("Reducing iterations too: {}", lessIterations);
         configuration.setIterations(lessIterations);
         configuration.setWarmup(0);
      } else {
         if (configuration.getRepetitions() > 5) {
            final int reducedRepetitions = configuration.getRepetitions() / 5;
            LOG.debug("Reducing repetitions to " + reducedRepetitions);
            configuration.setRepetitions(reducedRepetitions);
         } else {
            LOG.error("Cannot reduce iterations ({}) or repetitions ({}) anymore", configuration.getIterations(), configuration.getRepetitions());
            shouldBreak = true;
         }
      }
      return shouldBreak;
   }

   public Result getLastResult(final String version, final TestCase testcase, final int vmid) throws JAXBException {
      final File resultFile = getCurrentOrganizer().getResultFile(testcase, vmid, version);
      if (resultFile.exists()) {
         final Kopemedata data = new XMLDataLoader(resultFile).getFullData();
         final Result lastResult = data.getTestcases().getTestcase().get(0).getDatacollector().get(0).getResult().get(0);
         return lastResult;
      } else {
         LOG.debug("Resultfile {} does not exist", resultFile);
         return null;
      }
   }

   public void postEvaluate() {
      final File cleanFolder = new File(folders.getCleanFolder(), configuration.getVersion() + File.separator +
            configuration.getVersionOld() + File.separator +
            getCurrentOrganizer().getTest().getClazz() + File.separator +
            getCurrentOrganizer().getTest().getMethod());
      final Cleaner cleaner = new Cleaner(cleanFolder);
      for (final File clazzFile : folders.getDetailResultFolder().listFiles()) {
         final Map<String, TestData> testdata = DataReader.readClassFolder(clazzFile);
         for (final Map.Entry<String, TestData> entry : testdata.entrySet()) {
            cleaner.processTestdata(entry.getValue());
         }
      }
   }

   public void runOneComparison(final File logFolder, final TestCase testcase, final int vmid)
         throws IOException, InterruptedException, JAXBException, XmlPullParserException {
      String[] versions = getVersions();

      if (configuration.getMeasurementStrategy().equals(MeasurementStrategy.SEQUENTIAL)) {
         LOG.info("Running sequential");
         runSequential(logFolder, testcase, vmid, versions);
      } else if (configuration.getMeasurementStrategy().equals(MeasurementStrategy.PARALLEL)) {
         LOG.info("Running parallel");
         runParallel(logFolder, testcase, vmid, versions);
      }
   }

   private String[] getVersions() {
      String versions[] = new String[2];
      versions[0] = configuration.getVersionOld().equals("HEAD~1") ? configuration.getVersion() + "~1" : configuration.getVersionOld();
      versions[1] = configuration.getVersion();
      return versions;
   }

   private void runParallel(final File logFolder, final TestCase testcase, final int vmid, final String[] versions) throws InterruptedException {
      final ResultOrganizerParallel organizer = new ResultOrganizerParallel(folders, configuration.getVersion(), currentChunkStart, configuration.isUseKieker(), configuration.isSaveAll(), testcase,
            configuration.getIterations());
      currentOrganizer = organizer;
      final Thread[] threads = new Thread[2];
      for (int i = 0; i < 2; i++) {
         final String version = versions[i];
         threads[i] = new Thread(new Runnable() {
            @Override
            public void run() {
               try {
                  final File projectFolderTemp = new File(folders.getTempProjectFolder(), "parallel_" + version);
                  final PeASSFolders temporaryFolders;
                  if (!projectFolderTemp.exists()) {
                     temporaryFolders = TemporaryProjectFolderUtil.cloneForcefully(folders, projectFolderTemp);
                  } else {
                     temporaryFolders = new PeASSFolders(projectFolderTemp);
                  }
                  organizer.setFolder(version, temporaryFolders);
                  final TestExecutor testExecutor = getExecutor(temporaryFolders, version);
                  final OnceRunner runner = new OnceRunner(temporaryFolders, vcs, testExecutor, organizer, DependencyTester.this);
                  runner.runOnce(testcase, version, vmid, logFolder);
               } catch (IOException | InterruptedException | JAXBException | XmlPullParserException e) {
                  e.printStackTrace();
               }
            }
         });
         threads[i].start();
      }
      for (int i = 0; i < 2; i++) {
         threads[i].join();
      }
   }

   private void runSequential(final File logFolder, final TestCase testcase, final int vmid, final String versions[])
         throws IOException, InterruptedException, JAXBException, XmlPullParserException {
      currentOrganizer = new ResultOrganizer(folders, configuration.getVersion(), currentChunkStart, configuration.isUseKieker(), configuration.isSaveAll(), 
            testcase, configuration.getIterations());
      for (String version : versions) {
         runOnce(testcase, version, vmid, logFolder);
      }
   }

   public File getLogFolder(final String version, final TestCase testcase) {
      File logFolder = new File(folders.getLogFolder(), version + File.separator + testcase.getMethod());
      if (logFolder.exists()) {
         logFolder = new File(folders.getLogFolder(), version + File.separator + testcase.getMethod() + "_new");
      }
      logFolder.mkdirs();
      return logFolder;
   }

   public void runOnce(final TestCase testcase, final String version, final int vmid, final File logFolder)
         throws IOException, InterruptedException, JAXBException, XmlPullParserException {
      final TestExecutor testExecutor = getExecutor(folders, version);
      final OnceRunner runner = new OnceRunner(folders, vcs, testExecutor, getCurrentOrganizer(), this);
      runner.runOnce(testcase, version, vmid, logFolder);
   }

   protected synchronized TestExecutor getExecutor(final PeASSFolders currentFolders, final String version) {
      final JUnitTestTransformer testTransformer = new JUnitTestTransformer(currentFolders.getProjectFolder(), configuration);
      final TestExecutor testExecutor = ExecutorCreator.createExecutor(currentFolders, testTransformer, env);
      return testExecutor;
   }

   /**
    * This method can be overriden in order to handle kieker results before they are compressed
    * 
    * @param folder
    */
   @Override
   public void handleKiekerResults(final String version, final File folder) {

   }

   public void setVersions(final String version, final String versionOld) {
      configuration.setVersion(version);
      configuration.setVersionOld(versionOld);
   }

   protected boolean checkIsDecidable(final TestCase testcase, final int vmid) throws JAXBException {
      return false;
   }

   public ResultOrganizer getCurrentOrganizer() {
      return currentOrganizer;
   }
}
