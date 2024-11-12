package de.dagere.peass.measurement.dependencyprocessors;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionControlSystem;

public abstract class AbstractMeasurementProcessRunner {
   private static final Logger LOG = LogManager.getLogger(AbstractMeasurementProcessRunner.class);
   
   protected final VersionControlSystem vcs;
   protected final PeassFolders folders;
   
   public AbstractMeasurementProcessRunner(PeassFolders folders) {
      this.folders = folders;
      this.vcs = folders.getVCS();
   }
   
   public abstract void runOnce(final TestMethodCall testcase, final String commit, final int vmid, final File logFolder);
   
   protected void initCommit(final String commit) {
      if (vcs.equals(VersionControlSystem.SVN)) {
         throw new RuntimeException("SVN not supported currently.");
      } else {
         GitUtils.goToCommit(commit, folders.getProjectFolder());
      }
   }

   protected File initVMFolder(final String commit, final int vmid, final File logFolder) {
      File vmidFolder = new File(logFolder, "vm_" + vmid + "_" + commit);
      vmidFolder.mkdirs();

      LOG.info("Initial checkout finished, VM-Folder " + vmidFolder.getAbsolutePath() + " exists: " + vmidFolder.exists());
      return vmidFolder;
   }
   
   void cleanup() {
      emptyFolder(folders.getTempDir());
      emptyFolder(folders.getKiekerTempFolder());
      System.gc();
      try {
         Thread.sleep(1);
      } catch (InterruptedException e) {
         throw new RuntimeException(e);
      }
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
}
