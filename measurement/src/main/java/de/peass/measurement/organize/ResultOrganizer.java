package de.peass.measurement.organize;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
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
import de.dagere.kopeme.generated.TestcaseType;
import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.analysis.MultipleVMTestUtil;

public class ResultOrganizer {


   private static final Logger LOG = LogManager.getLogger(ResultOrganizer.class);

   private PeASSFolders folders;
   private String mainVersion;
   private long currentChunkStart;
   private boolean isUseKieker;
   private int thresholdForZippingInMB = 5;
   private final FolderDeterminer determiner;
   private boolean saveAll = false;

   public ResultOrganizer(final PeASSFolders folders, final String currentVersion, final long currentChunkStart, final boolean isUseKieker, final boolean saveAll) {
      super();
      this.folders = folders;
      this.mainVersion = currentVersion;
      this.currentChunkStart = currentChunkStart;
      this.isUseKieker = isUseKieker;
      this.saveAll = saveAll;

      determiner = new FolderDeterminer(folders);
   }

   public void saveResultFiles(final TestCase searchedTest, final String version, final int vmid)
         throws JAXBException, IOException {
      final File folder = getTempResultsFolder(searchedTest);
      if (folder != null) {
         final String methodname = searchedTest.getMethod();
         final File oneResultFile = new File(folder, methodname + ".xml");
         if (!oneResultFile.exists()) {
            LOG.debug("File {} does not exist.", oneResultFile.getAbsolutePath());
         } else {
            LOG.debug("Reading: {}", oneResultFile);
            final XMLDataLoader xdl = new XMLDataLoader(oneResultFile);
            final Kopemedata oneResultData = xdl.getFullData();
            final List<TestcaseType> testcaseList = oneResultData.getTestcases().getTestcase();
            final String clazz = oneResultData.getTestcases().getClazz();
            final TestCase realTestcase = new TestCase(clazz, methodname);
            if (testcaseList.size() > 0) {
               saveResults(version, vmid, realTestcase, oneResultFile, oneResultData, testcaseList);
            } else {
               LOG.error("No data - measurement failed?");
            }
            if (isUseKieker) {
               saveKiekerFiles(folder, folders.getFullResultFolder(realTestcase, mainVersion, version));
            }
         }
      }
      for (final File file : folders.getTempMeasurementFolder().listFiles()) {
         FileUtils.forceDelete(file);
      }
   }

   public File getTempResultsFolder(final TestCase searchedTest) {
      LOG.info("Searching method: {}", searchedTest);
      final String expectedFolderName = "*" + searchedTest.getClazz();
      final Collection<File> folderCandidates = findFolder(folders.getTempMeasurementFolder(), new WildcardFileFilter(expectedFolderName));
      if (folderCandidates.size() != 1) {
         LOG.error("Ordner {} ist {} mal vorhanden.", expectedFolderName, folderCandidates.size());
         return null;
      } else {
         final File folder = folderCandidates.iterator().next();
         return folder;
      }
   }

   private void saveResults(final String version, final int vmid, final TestCase testcase, final File oneResultFile,
         final Kopemedata oneResultData, final List<TestcaseType> testcaseList)
         throws JAXBException, IOException {
      // Update testname, in case it has been set to
      // testRepetition
      testcaseList.get(0).setName(testcase.getMethod());
      XMLDataStorer.storeData(oneResultFile, oneResultData);

      saveSummaryFile(version, testcase, testcaseList);

      final File destFile = determiner.getResultFile(testcase, vmid, version, mainVersion);
      LOG.info("Verschiebe nach: {}", destFile);
      if (!destFile.exists()) {
         FileUtils.moveFile(oneResultFile, destFile);
      } else {
         throw new RuntimeException("Moving failed: " + destFile + " already exist.");
      }
   }

   public void saveSummaryFile(final String version, final TestCase testcase, final List<TestcaseType> testcaseList) throws JAXBException {
      final TestcaseType oneRundata = testcaseList.get(0);
      final String shortClazzName = testcase.getShortClazz();
      final File fullResultFile = new File(folders.getFullMeasurementFolder(), shortClazzName + "_" + testcase.getMethod() + ".xml");
      MultipleVMTestUtil.saveSummaryData(fullResultFile, oneRundata, testcase, version, currentChunkStart);
   }

   private void saveKiekerFiles(final File folder, final File destFolder) throws IOException {
      final File kiekerFolder = folder.listFiles()[0];
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
         kiekerFolder.renameTo(dest);
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
}
