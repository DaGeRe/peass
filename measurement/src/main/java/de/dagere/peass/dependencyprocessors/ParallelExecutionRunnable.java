package de.dagere.peass.dependencyprocessors;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.execution.TestExecutor;
import de.dagere.peass.dependency.traces.TemporaryProjectFolderUtil;
import de.dagere.peass.measurement.organize.ResultOrganizerParallel;

public class ParallelExecutionRunnable implements Runnable {
   
   private ResultOrganizerParallel organizer;
   private final String version;
   private final TestCase testcase;
   private final int vmid;
   private final File logFolder;
   private final DependencyTester tester;
   private final PeassFolders temporaryFolders;
   
   public ParallelExecutionRunnable(final ResultOrganizerParallel organizer, final String version, final TestCase testcase, final int vmid, final File logFolder, final DependencyTester tester) throws IOException, InterruptedException {
      this.organizer = organizer;
      this.version = version;
      this.testcase = testcase;
      this.vmid = vmid;
      this.logFolder = logFolder;
      this.tester = tester;
      temporaryFolders = cloneProjectFolder();
   }

   @Override
   public void run() {
      try {
         final TestExecutor testExecutor = tester.getExecutor(temporaryFolders, version);
         final OnceRunner runner = new OnceRunner(temporaryFolders, testExecutor, organizer, tester);
         runner.runOnce(testcase, version, vmid, logFolder);
      } catch (IOException | InterruptedException | JAXBException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   private PeassFolders cloneProjectFolder() throws IOException, InterruptedException {
      final File projectFolderTemp = new File(tester.getFolders().getTempProjectFolder(), "parallel_" + version);
      final PeassFolders temporaryFolders;
      if (!projectFolderTemp.exists()) {
         temporaryFolders = TemporaryProjectFolderUtil.cloneForcefully(tester.getFolders(), projectFolderTemp);
      } else {
         temporaryFolders = new PeassFolders(projectFolderTemp);
      }
      organizer.addVersionFolders(version, temporaryFolders);
      return temporaryFolders;
   }
}
