package de.peass.measurement.organize;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.datastorage.XMLDataStorer;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata;
import de.dagere.kopeme.generated.TestcaseType;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.analysis.MultipleVMTestUtil;

public class ResultOrganizer {

   private static final Logger LOG = LogManager.getLogger(ResultOrganizer.class);

   private final PeASSFolders folders;
   private final String mainVersion;
   private final long currentChunkStart;
   private final boolean isUseKieker;
   private int thresholdForZippingInMB = 5;
   private final FolderDeterminer determiner;
   private boolean saveAll = false;
   protected final TestCase testcase;
   private boolean success = true;
   private final int expectedIterations;

   public ResultOrganizer(final PeASSFolders folders, final String currentVersion, final long currentChunkStart, final boolean isUseKieker, final boolean saveAll, final TestCase test,
         final int expectedIterations) {
      this.folders = folders;
      this.mainVersion = currentVersion;
      this.currentChunkStart = currentChunkStart;
      this.isUseKieker = isUseKieker;
      this.saveAll = saveAll;
      this.testcase = test;
      this.expectedIterations = expectedIterations;

      determiner = new FolderDeterminer(folders);
   }

   //TODO the success test duplicatees saveResultFiles code and logic from DependencyTester.shouldReduce
   /**
    * Tests whether there is a correct result file, i.e. a XML file in the correct position with the right amount of iterations (it may be less iterations if the test takes too long).
    * This only works *before* the result has been moved, afterwards, the file will be gone and the measurement will be considered no success
    * @return true of the measurement was correct
    */
   public boolean testSuccess(final String version) {
      final File folder = getTempResultsFolder(version);
      if (folder != null) {
         final String methodname = testcase.getMethod();
         final File oneResultFile = new File(folder, methodname + ".xml");
         try {
            if (!oneResultFile.exists()) {
               success = false;
               LOG.error("Result file {} does not exist - probably timeout", oneResultFile);
            } else {
               LOG.debug("Reading: {}", oneResultFile);
               XMLDataLoader xdl = new XMLDataLoader(oneResultFile);
               final Kopemedata oneResultData = xdl.getFullData();
               final List<TestcaseType> testcaseList = oneResultData.getTestcases().getTestcase();
               if (testcaseList.size() > 0) {
                  Result result = oneResultData.getTestcases().getTestcase().get(0).getDatacollector().get(0).getResult().get(0);
                  if (result.getIterations() == expectedIterations) {
                     success = true;
                  } else {
                     success = false;
                     LOG.error("Wrong execution count: {} Expected: {}", result.getIterations(), expectedIterations);
                  }
               } else {
                  LOG.error("Testcase not found in XML");
                  success = false;
               }
            }
         } catch (JAXBException e) {
            e.printStackTrace();
            success = false;
         }
      } else {
         LOG.error("Folder {} does not exist", folder);
         success = false;
      }
      return success;
   }

   public void saveResultFiles(final String version, final int vmid)
         throws JAXBException, IOException {
      // Saving and merging result files should not be executed in parallel, therefore, this needs to be synchronized over the class (not the instance)
      synchronized (ResultOrganizer.class) {
         final File folder = getTempResultsFolder(version);
         if (folder != null) {
            final String methodname = testcase.getMethod();
            final File oneResultFile = new File(folder, methodname + ".xml");
            if (!oneResultFile.exists()) {
               LOG.debug("File {} does not exist.", oneResultFile.getAbsolutePath());
               success = false;
            } else {
               LOG.debug("Reading: {}", oneResultFile);
               final XMLDataLoader xdl = new XMLDataLoader(oneResultFile);
               final Kopemedata oneResultData = xdl.getFullData();
               final List<TestcaseType> testcaseList = oneResultData.getTestcases().getTestcase();
               if (testcaseList.size() > 0) {
                  saveResults(version, vmid, oneResultFile, oneResultData, testcaseList);

                  if (isUseKieker) {
                     File destFolder = folders.getFullResultFolder(testcase, mainVersion, version);
                     saveKiekerFiles(folder, destFolder);
                  }
               } else {
                  LOG.error("No data - measurement failed?");
                  success = false;
               }
            }
            for (final File file : folder.listFiles()) {
               FileUtils.forceDelete(file);
            }
         }
      }
   }

   public File getTempResultsFolder(final String version) {
      LOG.info("Searching method: {}", testcase);
      final String expectedFolderName = "*" + testcase.getClazz();
      final Collection<File> folderCandidates = findFolder(folders.getTempMeasurementFolder(), new WildcardFileFilter(expectedFolderName));
      if (folderCandidates.size() != 1) {
         LOG.error("Ordner {} ist {} mal vorhanden.", expectedFolderName, folderCandidates.size());
         return null;
      } else {
         final File folder = folderCandidates.iterator().next();
         return folder;
      }
   }

   private void saveResults(final String version, final int vmid, final File oneResultFile,
         final Kopemedata oneResultData, final List<TestcaseType> testcaseList)
         throws JAXBException, IOException {
      // Update testname, in case it has been set to
      // testRepetition
      testcaseList.get(0).setName(testcase.getMethod());

      saveSummaryFile(version, testcaseList, oneResultFile);

      final File destFile = determiner.getResultFile(testcase, vmid, version, mainVersion);
      destFile.getParentFile().mkdirs();
      LOG.info("Saving in: {}", destFile);
      if (!destFile.exists()) {
         final Fulldata fulldata = oneResultData.getTestcases().getTestcase().get(0).getDatacollector().get(0).getResult().get(0).getFulldata();
         if (fulldata.getFileName() != null) {
            saveFulldataFile(vmid, oneResultFile, destFile, fulldata);
         }
         XMLDataStorer.storeData(destFile, oneResultData);
         oneResultFile.delete();
      } else {
         throw new RuntimeException("Moving failed: " + destFile + " already exist.");
      }
   }

   private void saveFulldataFile(final int vmid, final File oneResultFile, final File destFile, final Fulldata fulldata) throws IOException {
      File fulldataFile = new File(oneResultFile.getParentFile(), fulldata.getFileName());
      final String destFileName = testcase.getMethod() + "_kopeme_" + vmid + ".tmp";
      File destKopemeFile = new File(destFile.getParentFile(), destFileName);
      Files.move(fulldataFile.toPath(), destKopemeFile.toPath());
      fulldata.setFileName(destFileName);
   }

   public void saveSummaryFile(final String version, final List<TestcaseType> testcaseList, final File oneResultFile) throws JAXBException {
      final TestcaseType oneRundata = testcaseList.get(0);
      final String shortClazzName = testcase.getShortClazz();
      final File fullResultFile = new File(folders.getFullMeasurementFolder(), shortClazzName + "_" + testcase.getMethod() + ".xml");
      MultipleVMTestUtil.saveSummaryData(fullResultFile, oneResultFile, oneRundata, testcase, version, currentChunkStart);
   }

   private void saveKiekerFiles(final File folder, final File destFolder) throws IOException {
      final File kiekerFolder = folder.listFiles()[0];
      if (!kiekerFolder.getName().matches("[0-9]*")) {
         throw new RuntimeException("Kieker folder is expected to consist only of numbers, but was " + kiekerFolder.getName());
      }
      if (saveAll) {
         moveOrCompressFile(destFolder, kiekerFolder);
      } else {
         FileUtils.deleteDirectory(kiekerFolder);
      }
   }

   private void moveOrCompressFile(final File destFolder, final File kiekerFolder) throws IOException {
      final long size = FileUtils.sizeOf(kiekerFolder);
      final long sizeInMb = size / (1024 * 1024);
      LOG.debug("Kieker folder size: {} MB ({})", sizeInMb, size);
      if (sizeInMb > thresholdForZippingInMB) {
         final File dest = new File(destFolder, kiekerFolder.getName() + ".tar");
         final ProcessBuilder processBuilder = new ProcessBuilder("tar", "-czf", dest.getAbsolutePath(), kiekerFolder.getAbsolutePath());
         processBuilder.environment().put("GZIP", "-9");
         final Process process = processBuilder.start();
         try {
            process.waitFor();
         } catch (final InterruptedException e) {
            e.printStackTrace();
         }
         FileUtils.deleteDirectory(kiekerFolder);
      } else {
         final File dest = new File(destFolder, kiekerFolder.getName());
         if (!kiekerFolder.renameTo(dest)) {
            LOG.error("Renaming {} to {} failed",
                  kiekerFolder.getAbsolutePath(),
                  dest.getAbsolutePath());
         }
      }
   }

   protected static List<File> findFolder(final File baseFolder, final FileFilter folderFilter) {
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

   public int getThresholdForZippingInMB() {
      return thresholdForZippingInMB;
   }

   public void setThresholdForZippingInMB(final int thresholdForZippingInMB) {
      this.thresholdForZippingInMB = thresholdForZippingInMB;
   }

   public File getResultFile(final TestCase testcase, final int vmid, final String version) {
      return determiner.getResultFile(testcase, vmid, version, mainVersion);
   }

   public boolean isSaveAll() {
      return saveAll;
   }

   public void setSaveAll(final boolean saveAll) {
      this.saveAll = saveAll;
   }

   public TestCase getTest() {
      return testcase;
   }

   public boolean isSuccess() {
      return success;
   }
}
