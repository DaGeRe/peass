package de.dagere.peass.measurement.organize;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class FolderDeterminer {
   
   private static final Logger LOG = LogManager.getLogger(FolderDeterminer.class);
   
   private PeASSFolders folders;
   
   public FolderDeterminer(final PeASSFolders folders) {
      this.folders = folders;
   }

   public File getResultFile(final TestCase testcase, final int vmid, final String version, final String mainVersion) {
      final File compareVersionFolder = folders.getFullResultFolder(testcase, mainVersion, version);
      final File destFile = new File(compareVersionFolder, testcase.getMethod() + "_" + vmid + "_" + version + ".xml");
      return destFile;
   }
   
   public void testResultFolders(final String version, final String versionOld, final TestCase testcase) {
      final File resultFile = getResultFile(testcase, 0, version, version);
      if (resultFile.exists()) {
         throw new RuntimeException("File " + resultFile.getAbsolutePath() + " exists - please remove data from old measurement!");
      }
      final File resultFile2 = getResultFile(testcase, 0, versionOld, version);
      if (resultFile2.exists()) {
         throw new RuntimeException("File " + resultFile2.getAbsolutePath() + " exists - please remove data from old measurement!");
      }
      LOG.trace("Tested: {} {}", resultFile.getAbsolutePath(), resultFile2.getAbsolutePath());
   }
}
