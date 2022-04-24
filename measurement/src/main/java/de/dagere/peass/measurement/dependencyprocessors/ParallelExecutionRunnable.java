package de.dagere.peass.measurement.dependencyprocessors;

import java.io.File;
import java.io.IOException;

import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
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
      } catch (IOException | InterruptedException  | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   private PeassFolders cloneProjectFolder() throws IOException, InterruptedException {
      PeassFolders temporaryFolders = tester.getFolders().getTempFolder("parallel_" + version);
      organizer.addVersionFolders(version, temporaryFolders);
      return temporaryFolders;
   }
}
