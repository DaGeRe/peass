package de.peass.measurement.organize;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;

public class FolderDeterminer {
   
   private static final Logger LOG = LogManager.getLogger(FolderDeterminer.class);
   
   private PeASSFolders folders;
   
   public FolderDeterminer(PeASSFolders folders) {
      this.folders = folders;
   }

   public File getResultFile(final TestCase testcase, final int vmid, final String version, String mainVersion) {
      final File compareVersionFolder = getFullResultFolder(testcase, version, mainVersion);
      final File destFile = new File(compareVersionFolder, testcase.getMethod() + "_" + vmid + "_" + version + ".xml");
      return destFile;
   }

   public File getFullResultFolder(final TestCase testcase, final String version, String mainVersion) {
      final File destFolder = new File(folders.getDetailResultFolder(), testcase.getClazz());
      final File currentVersionFolder = new File(destFolder, mainVersion);
      if (!currentVersionFolder.exists()) {
         currentVersionFolder.mkdir();
      }
      final File compareVersionFolder = new File(currentVersionFolder, version);
      if (!compareVersionFolder.exists()) {
         compareVersionFolder.mkdir();
      }
      return compareVersionFolder;
   }
   
   public void testResultFolders(final String version, final String versionOld, final TestCase testcase) {
      File resultFile = getResultFile(testcase, 0, version, version);
      if (resultFile.exists()) {
         throw new RuntimeException("File " + resultFile.getAbsolutePath() + " exists - please remove data from old measurement!");
      }
      File resultFile2 = getResultFile(testcase, 0, versionOld, version);
      if (resultFile2.exists()) {
         throw new RuntimeException("File " + resultFile2.getAbsolutePath() + " exists - please remove data from old measurement!");
      }
      LOG.trace("Tested: {} {}", resultFile.getAbsolutePath(), resultFile2.getAbsolutePath());
   }
}
