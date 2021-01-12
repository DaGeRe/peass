package de.peass.dependencyprocessors;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.execution.TestExecutor;
import de.peass.measurement.organize.ResultOrganizer;
import de.peass.testtransformation.JUnitTestTransformer;
import de.peass.vcs.GitUtils;
import de.peass.vcs.VersionControlSystem;

public class OnceRunner {
   
   private static final Logger LOG = LogManager.getLogger(OnceRunner.class);
   
   protected final PeASSFolders folders;
   private final VersionControlSystem vcs;
   protected final JUnitTestTransformer testTransformer;
   protected final TestExecutor testExecutor;

   protected final ResultOrganizer currentOrganizer;
   private final KiekerResultHandler resultHandler;

   public OnceRunner(PeASSFolders folders, VersionControlSystem vcs, TestExecutor testExecutor, ResultOrganizer currentOrganizer, 
         KiekerResultHandler resultHandler) {
      this.folders = folders;
      this.vcs = vcs;
      this.testTransformer = testExecutor.getTestTransformer();
      this.testExecutor = testExecutor;
      this.currentOrganizer = currentOrganizer;
      this.resultHandler = resultHandler;
   }

   public void runOnce(final TestCase testcase, final String version, final int vmid, final File logFolder)
         throws IOException, InterruptedException, JAXBException, XmlPullParserException {
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
      final long outerTimeout = 5 + (int) (this.testTransformer.getConfig().getTimeoutInMinutes() * 1.1);
      testExecutor.executeTest(testcase, vmidFolder, outerTimeout);

      LOG.debug("Handling Kieker results");
      resultHandler.handleKiekerResults(version, currentOrganizer.getTempResultsFolder(version));

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
}
