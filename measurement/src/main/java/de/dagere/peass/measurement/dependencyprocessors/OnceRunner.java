package de.dagere.peass.measurement.dependencyprocessors;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.organize.ResultOrganizer;
import de.dagere.peass.testtransformation.TestTransformer;
import de.dagere.peass.vcs.GitUtils;
import de.dagere.peass.vcs.VersionControlSystem;


public class OnceRunner {

   private static final Logger LOG = LogManager.getLogger(OnceRunner.class);

   protected final PeassFolders folders;
   private final VersionControlSystem vcs;
   protected final TestTransformer testTransformer;
   protected final TestExecutor testExecutor;

   protected final ResultOrganizer currentOrganizer;
   private final KiekerResultHandler resultHandler;

   public OnceRunner(final PeassFolders folders, final TestExecutor testExecutor, final ResultOrganizer currentOrganizer, final KiekerResultHandler resultHandler) {
      this.folders = folders;
      this.vcs = folders.getVCS();
      this.testTransformer = testExecutor.getTestTransformer();
      this.testExecutor = testExecutor;
      this.currentOrganizer = currentOrganizer;
      this.resultHandler = resultHandler;
   }

   public void runOnce(final TestCase testcase, final String version, final int vmid, final File logFolder)
         throws IOException, InterruptedException, XmlPullParserException {
      if (vcs.equals(VersionControlSystem.SVN)) {
         throw new RuntimeException("SVN not supported currently.");
      } else {
         if (GitUtils.getName("HEAD", folders.getProjectFolder()) == null || !GitUtils.getName("HEAD", folders.getProjectFolder()).equals(version)) {
            GitUtils.goToTag(version, folders.getProjectFolder());
         }
      }

      final File vmidFolder = initVMFolder(version, vmid, logFolder);

      if (testTransformer.getConfig().isUseKieker()) {
         testExecutor.loadClasses();
      }
      testExecutor.prepareKoPeMeExecution(new File(logFolder, "clean.txt"));
      final long outerTimeout = 10 + (int) (this.testTransformer.getConfig().getTimeoutInSeconds() * 1.2);
      testExecutor.executeTest(testcase, vmidFolder, outerTimeout);

      LOG.debug("Handling Kieker results");
      resultHandler.handleKiekerResults(version, currentOrganizer.getTempResultsFolder(version));

      LOG.info("Organizing result paths");
      currentOrganizer.saveResultFiles(version, vmid);

      cleanup();
   }

   private File initVMFolder(final String version, final int vmid, final File logFolder) {
      File vmidFolder = new File(logFolder, "vm_" + vmid + "_" + version);
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
   
   public ResultOrganizer getCurrentOrganizer() {
      return currentOrganizer;
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
