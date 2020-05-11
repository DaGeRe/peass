package de.peass.dependencyprocessors;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datacollection.DataCollectorList;
import de.peass.dependency.ExecutorCreator;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.dependency.execution.TestExecutor;
import de.peass.measurement.analysis.Cleaner;
import de.peass.measurement.analysis.DataReader;
import de.peass.measurement.analysis.statistics.TestData;
import de.peass.measurement.organize.FolderDeterminer;
import de.peass.measurement.organize.ResultOrganizer;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.testtransformation.TimeBasedTestTransformer;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;

/**
 * Runs a PeASS with only running the tests where a changed class is present.
 * 
 * @author reichelt
 *
 */
public class DependencyTester {

   private static final Logger LOG = LogManager.getLogger(DependencyTester.class);

   protected final PeASSFolders folders;
   protected final MeasurementConfiguration configuration;
   // protected final int vms;

   private final VersionControlSystem vcs;

   protected final JUnitTestTransformer testTransformer;
   protected final TestExecutor testExecutor;

   protected String currentVersion;
   protected ResultOrganizer currentOrganizer;
   protected long currentChunkStart = 0;
   
   public DependencyTester(final PeASSFolders folders, final JUnitTestTransformer testgenerator) throws IOException {
      super();
      this.folders = folders;
      this.configuration = testgenerator.getConfig();

      vcs = VersionControlSystem.getVersionControlSystem(folders.getProjectFolder());
      this.testTransformer = testgenerator;
      testExecutor = ExecutorCreator.createExecutor(folders, Integer.MAX_VALUE, testTransformer);
   }

   /**
    * Compares the given testcase for the given versions.
    * 
    * @param version Current version to test
    * @param versionOld Old version to test
    * @param testcase Testcase to test
    */
   public void evaluate(final TestCase testcase) throws IOException, InterruptedException, JAXBException {
      new FolderDeterminer(folders).testResultFolders(configuration.getVersion(), configuration.getVersionOld(), testcase);
      
      LOG.info("Executing test " + testcase.getClazz() + " " + testcase.getMethod() + " in versions {} and {}", configuration.getVersionOld(), configuration.getVersion());

      final File logFolder = getLogFolder(configuration.getVersion(), testcase);

      currentChunkStart = System.currentTimeMillis();
      for (int vmid = 0; vmid < configuration.getVms(); vmid++) {
         runOneComparison(logFolder, testcase, vmid);
      }
   }

   public void postEvaluate() {
      final File cleanFolder = new File(folders.getCleanFolder(), configuration.getVersion() + File.separator + 
            configuration.getVersionOld() + File.separator + 
            currentOrganizer.getTest().getClazz() + File.separator +
            currentOrganizer.getTest().getMethod());
      final Cleaner cleaner = new Cleaner(cleanFolder);
      for (final File clazzFile : folders.getDetailResultFolder().listFiles()) {
         final Map<String, TestData> testdata = DataReader.readClassFolder(clazzFile);
         for (final Map.Entry<String, TestData> entry : testdata.entrySet()) {
            cleaner.processTestdata(entry.getValue());
         }
      }
   }

   public void runOneComparison(final File logFolder, final TestCase testcase, final int vmid)
         throws IOException, InterruptedException, JAXBException {
      currentVersion = configuration.getVersion();
      //TODO Vermutlich currentVersion -> mainVersion
      currentOrganizer = new ResultOrganizer(folders, configuration.getVersion(), currentChunkStart, testTransformer.getConfig().isUseKieker(), false, testcase);
      if (configuration.getVersionOld().equals("HEAD~1")) {
         runOnce(testcase, configuration.getVersion() + "~1", vmid, logFolder);
      } else {
         runOnce(testcase, configuration.getVersionOld(), vmid, logFolder);
      }

      runOnce(testcase, configuration.getVersion(), vmid, logFolder);
   }

   public File getLogFolder(final String version, final TestCase testcase) {
      File logFolder = new File(folders.getLogFolder(), version + File.separator + testcase.getMethod());
      if (logFolder.exists()) {
         logFolder = new File(folders.getLogFolder(), version + File.separator + testcase.getMethod() + "_new");
      }
      logFolder.mkdirs();
      return logFolder;
   }

   protected void runOnce(final TestCase testcase, final String version, final int vmid, final File logFolder)
         throws IOException, InterruptedException, JAXBException {
      if (vcs.equals(VersionControlSystem.SVN)) {
         throw new RuntimeException("SVN not supported currently.");
      } else {
         GitUtils.goToTag(version, folders.getProjectFolder());
      }

      final File vmidFolder = initVMFolder(version, vmid, logFolder);

      if (testTransformer.getConfig().isUseKieker()) {
         testExecutor.loadClasses();
      }
      testExecutor.prepareKoPeMeExecution(new File(logFolder, "clean.txt"));
      final long timeout = 5 + (int) (this.configuration.getTimeout() * 1.1);
      testExecutor.executeTest(testcase, vmidFolder, timeout);
      
      LOG.debug("Handling Kieker results");
      handleKiekerResults(version, currentOrganizer.getTempResultsFolder());
      
      LOG.info("Organizing result paths");
      currentOrganizer.saveResultFiles(version, vmid);

      cleanup();
   }

   private File initVMFolder(final String version, final int vmid, final File logFolder) {
      File vmidFolder = new File(logFolder, "vm_" + vmid + "_" + version);
      if (vmidFolder.exists()) {
         vmidFolder = new File(logFolder, "vm_" + vmid + "_" + version + "_new");
      }
      vmidFolder.mkdirs();

      LOG.info("Initial checkout finished, VM-Folder " + vmidFolder.getAbsolutePath() + " exists: " + vmidFolder.exists());
      return vmidFolder;
   }

   void cleanup() throws InterruptedException {
      emptyFolder(folders.getTempDir());
      emptyFolder(folders.getKiekerTempFolder());
      System.gc();
      Thread.sleep(1);
   }

   private void emptyFolder(final File tempDir) {
      for (final File createdTempFile : tempDir.listFiles()) {
         try {
            if (createdTempFile.isDirectory()) {
               FileUtils.deleteDirectory(createdTempFile);
            } else {
               createdTempFile.delete();
            }
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }
   }

   public int getVMCount() {
      return configuration.getVms();
   }
   
   /**
    * This method can be overriden in order to handle kieker results before they are compressed
    * @param folder 
    */
   protected void handleKiekerResults(final String version, final File folder) {
      
   }

   public void setVersions(final String version, final String versionOld) {
      configuration.setVersion(version);
      configuration.setVersionOld(versionOld);
   }

   protected boolean checkIsDecidable(final TestCase testcase, final int vmid) throws JAXBException {
      return false;
   }
}
