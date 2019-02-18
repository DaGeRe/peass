package de.peass.dependencyprocessors;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datacollection.DataCollectorList;
import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.TestcaseType;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.TestResultManager;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.TestExecutor;
import de.peass.measurement.analysis.Cleaner;
import de.peass.measurement.analysis.DataReader;
import de.peass.measurement.analysis.MultipleVMTestUtil;
import de.peass.measurement.analysis.statistics.TestData;
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
   protected final int vms;

   private final VersionControlSystem vcs;

   protected final JUnitTestTransformer testTransformer;
   protected final TestExecutor testExecutor;

   protected String currentVersion;
   protected long currentChunkStart = 0;
   private long timeout = 5;

   public DependencyTester(final PeASSFolders folders, final int duration, final int vms,
         final boolean runInitial, final int repetitions, final boolean useKieker) throws IOException {
      super();
      this.folders = folders;
      this.vms = vms;

      vcs = VersionControlSystem.getVersionControlSystem(folders.getProjectFolder());

      testTransformer = new TimeBasedTestTransformer(folders.getProjectFolder());
      ((TimeBasedTestTransformer) testTransformer).setDuration(duration);
      if (repetitions != 1) {
         testTransformer.setRepetitions(repetitions);
      }
      testTransformer.setDatacollectorlist(DataCollectorList.ONLYTIME);
      testTransformer.setIterations(0);
      testTransformer.setWarmupExecutions(0);
      testTransformer.setUseKieker(useKieker);
      testTransformer.setLogFullData(true);
      testExecutor = TestResultManager.createExecutor(folders, Integer.MAX_VALUE, testTransformer);
      // testExecutor = new MavenKiekerTestExecutor(folders, testTransformer, Integer.MAX_VALUE);
   }

   public DependencyTester(final PeASSFolders folders, final boolean runInitial, final JUnitTestTransformer testgenerator, final int vms) throws IOException {
      super();
      this.folders = folders;
      this.vms = vms;

      vcs = VersionControlSystem.getVersionControlSystem(folders.getProjectFolder());
      this.testTransformer = testgenerator;
      testExecutor = TestResultManager.createExecutor(folders, Integer.MAX_VALUE, testTransformer);
   }

   /**
    * Compares the given testcase for the given versions.
    * 
    * @param version Current version to test
    * @param versionOld Old version to test
    * @param testcase Testcase to test
    */
   public void evaluate(final String version, final String versionOld, final TestCase testcase) throws IOException, InterruptedException, JAXBException {
      LOG.info("Executing test " + testcase.getClazz() + " " + testcase.getMethod() + " in versions {} and {}", versionOld, version);

      final File logFolder = getLogFolder(version, testcase);

      currentChunkStart = System.currentTimeMillis();
      for (int vmid = 0; vmid < vms; vmid++) {
         runOneComparison(version, versionOld, logFolder, testcase, vmid);
      }
   }

   public void postEvaluate() {
      for (final File clazzFile : folders.getDetailResultFolder().listFiles()) {
         final Map<String, TestData> testdata = DataReader.readClassFolder(clazzFile);
         for (final Map.Entry<String, TestData> entry : testdata.entrySet()) {
            new Cleaner(folders.getCleanFolder()).processTestdata(entry.getValue());
         }
      }

   }

   public void runOneComparison(final String version, final String versionOld, final File logFolder, final TestCase testset, final int vmid)
         throws IOException, InterruptedException, JAXBException {
      currentVersion = version;
      if (versionOld.equals("HEAD~1")) {
         runOnce(testset, version + "~1", vmid, logFolder);
      } else {
         runOnce(testset, versionOld, vmid, logFolder);
      }

      runOnce(testset, version, vmid, logFolder);
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

      File vmidFolder = new File(logFolder, "vm_" + vmid + "_" + version);
      if (vmidFolder.exists()) {
         vmidFolder = new File(logFolder, "vm_" + vmid + "_" + version + "_new");
      }
      vmidFolder.mkdirs();

      LOG.info("Initial checkout finished, VM-Folder " + vmidFolder.getAbsolutePath() + " exists: " + vmidFolder.exists());

      testExecutor.prepareKoPeMeExecution(new File(logFolder, "clean.txt"));
      final long timeout = 5 + (int) (this.timeout * 1.1);
      testExecutor.executeTest(testcase, vmidFolder, timeout);

      LOG.info("Ändere eine Klassen durch Ergänzung des Gitversion-Elements.");
      saveResultFiles(testcase, version, vmid);
      System.gc();
      Thread.sleep(1);
   }

   private void saveResultFiles(final TestCase testset, final String version, final int vmid)
         throws JAXBException, IOException {
      // for (final Map.Entry<ChangedEntity, List<String>> testcaseEntry : testset.entrySet()) {
      LOG.info("Teste Methoden: {}", 1);
      final String expectedFolderName = "*" + testset.getClazz();
      final Collection<File> folderCandidates = findFolder(folders.getTempMeasurementFolder(), new WildcardFileFilter(expectedFolderName));
      if (folderCandidates.size() != 1) {
         LOG.error("Ordner {} ist {} mal vorhanden.", expectedFolderName, folderCandidates.size());
      } else {
         final File folder = folderCandidates.iterator().next();
         final String methodname = testset.getMethod();
         final File oneResultFile = new File(folder, methodname + ".xml");
         if (!oneResultFile.exists()) {
            LOG.debug("Datei {} existiert nicht.", oneResultFile.getAbsolutePath());
         } else {
            LOG.debug("Lese: {}", oneResultFile);
            final XMLDataLoader xdl = new XMLDataLoader(oneResultFile);
            final Kopemedata oneResultData = xdl.getFullData();
            final List<TestcaseType> testcaseList = oneResultData.getTestcases().getTestcase();
            final String clazz = oneResultData.getTestcases().getClazz();
            if (testcaseList.size() > 0) {
               saveResults(version, vmid, new TestCase(clazz, methodname), oneResultFile, oneResultData, testcaseList);
            } else {
               LOG.error("Keine Daten vorhanden - Messung fehlgeschlagen?");
            }
         }
         if (testTransformer.isUseKieker()) {
            final File methodFolder = new File(folders.getTempMeasurementFolder(), testset.getClazz() + "." + methodname);
            if (!methodFolder.exists()) {
               methodFolder.mkdir();
            }
            final File versionFolder = new File(methodFolder, version);
            if (!versionFolder.exists()) {
               versionFolder.mkdir();
            }

            final File dest = new File(versionFolder, vmid + ".tar.gz");

            try {
               final Process process = new ProcessBuilder("tar", "-czf", dest.getAbsolutePath(),
                     folder.getAbsolutePath()).start();
               process.waitFor();
               FileUtils.deleteDirectory(folder);
            } catch (final InterruptedException e) {
               e.printStackTrace();
            }
         }
      }
      for (final File file : folders.getTempMeasurementFolder().listFiles()) {
         FileUtils.forceDelete(file);
      }
   }

   public File getResultFile(final TestCase testcase, final int vmid, final String version) {
      final File destFolder = new File(folders.getDetailResultFolder(), testcase.getClazz());
      final File currentVersionFolder = new File(destFolder, currentVersion);
      if (!currentVersionFolder.exists()) {
         currentVersionFolder.mkdir();
      }
      final File compareVersionFolder = new File(currentVersionFolder, version);
      if (!compareVersionFolder.exists()) {
         compareVersionFolder.mkdir();
      }
      final File destFile = new File(compareVersionFolder, testcase.getMethod() + "_" + vmid + "_" + version + ".xml");
      return destFile;
   }

   private void saveResults(final String version, final int vmid, final TestCase testcase, final File oneResultFile,
         final Kopemedata oneResultData, final List<TestcaseType> testcaseList)
         throws JAXBException, IOException {
      // Update testname, in case it has been set to
      // testRepetition
      testcaseList.get(0).setName(testcase.getMethod());
      XMLDataStorer.storeData(oneResultFile, oneResultData);

      final TestcaseType oneRundata = testcaseList.get(0);
      final String shortClazzName = testcase.getShortClazz();
      final File fullResultFile = new File(folders.getFullMeasurementFolder(), shortClazzName + "_" + testcase.getMethod() + ".xml");
      MultipleVMTestUtil.fillOtherData(fullResultFile, oneRundata, testcase, version, currentChunkStart);
      final File destFile = getResultFile(testcase, vmid, version);
      LOG.info("Verschiebe nach: {}", destFile);
      if (!destFile.exists()) {
         FileUtils.moveFile(oneResultFile, destFile);
      } else {
         throw new RuntimeException("Moving failed: " + destFile + " already exist.");
      }
   }

   private static List<File> findFolder(final File baseFolder, final FileFilter folderFilter) {
      final List<File> files = new LinkedList<>();
      for (final File f : baseFolder.listFiles()) {
         if (f.isDirectory()) {
            if (folderFilter.accept(f)) {
               files.add(f);
            } else {
               files.addAll(findFolder(f, folderFilter));
            }
         }
      }
      return files;
   }

   public int getVMCount() {
      return vms;
   }

   public void setTimeout(final long timeout) {
      this.timeout = timeout;
      testTransformer.setSumTime(60 * 1000 * timeout);
   }

}
