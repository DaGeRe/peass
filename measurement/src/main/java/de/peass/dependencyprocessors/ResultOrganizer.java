package de.peass.dependencyprocessors;

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
   private String currentVersion;
   private long currentChunkStart;
   private boolean isUseKieker;
   
   public ResultOrganizer(PeASSFolders folders, String currentVersion, long currentChunkStart, boolean isUseKieker) {
      super();
      this.folders = folders;
      this.currentVersion = currentVersion;
      this.currentChunkStart = currentChunkStart;
      this.isUseKieker = isUseKieker;
   }

   public void saveResultFiles(final TestCase testset, final String version, final int vmid)
         throws JAXBException, IOException {
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
         if (isUseKieker) {
            saveKiekerFiles(testset, version, vmid, folder, methodname);
         }
      }
      for (final File file : folders.getTempMeasurementFolder().listFiles()) {
         FileUtils.forceDelete(file);
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
      
      final File destFile = getResultFile(testcase, vmid, version);
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
   
   private void saveKiekerFiles(final TestCase testset, final String version, final int vmid, final File folder, final String methodname) throws IOException {
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
}
