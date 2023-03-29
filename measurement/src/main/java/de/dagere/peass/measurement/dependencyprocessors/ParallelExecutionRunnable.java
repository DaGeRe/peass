package de.dagere.peass.measurement.dependencyprocessors;

import java.io.File;
import java.io.IOException;

import de.dagere.nodeDiffGenerator.data.TestMethodCall;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.organize.ResultOrganizerParallel;

public class ParallelExecutionRunnable implements Runnable {

   private ResultOrganizerParallel organizer;
   private final String commit;
   private final TestMethodCall testcase;
   private final int vmid;
   private final File logFolder;
   private final DependencyTester tester;
   private final PeassFolders temporaryFolders;

   public ParallelExecutionRunnable(final ResultOrganizerParallel organizer, final String commit, final TestMethodCall testcase, final int vmid, final File logFolder,
         final DependencyTester tester, final String gitCryptKey) throws IOException {
      this.organizer = organizer;
      this.commit = commit;
      this.testcase = testcase;
      this.vmid = vmid;
      this.logFolder = logFolder;
      this.tester = tester;
      temporaryFolders = cloneProjectFolder(gitCryptKey);
   }

   @Override
   public void run() {
      final TestExecutor testExecutor = tester.getExecutor(temporaryFolders, commit);
      final OnceRunner runner = new OnceRunner(temporaryFolders, testExecutor, organizer, tester);
      runner.runOnce(testcase, commit, vmid, logFolder);
   }

   private PeassFolders cloneProjectFolder(final String gitCryptKey) throws IOException {
      PeassFolders temporaryFolders = tester.getFolders().getTempFolder("parallel_" + commit, gitCryptKey);
      organizer.addCommitFolders(commit, temporaryFolders);
      return temporaryFolders;
   }
}
