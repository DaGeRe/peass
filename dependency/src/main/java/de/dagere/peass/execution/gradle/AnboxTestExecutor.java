package de.dagere.peass.execution.gradle;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.execution.processutils.ProcessBuilderHelper;
import de.dagere.peass.execution.utils.CommandConcatenator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.utils.StreamGobbler;

public class AnboxTestExecutor extends GradleTestExecutor {

   public static final String ANBOX_EMULATOR_FOLDER_BASE = "/storage/emulated/0/Documents/peass/";
   public static final String ANBOX_EMULATOR_FOLDER_TEMP_RESULT = ANBOX_EMULATOR_FOLDER_BASE + "measurementsTemp/";

   private static final Logger LOG = LogManager.getLogger(AnboxTestExecutor.class);

   public AnboxTestExecutor(final PeassFolders folders, final JUnitTestTransformer testTransformer, final EnvironmentVariables env) {
      super(folders, testTransformer, env);
   }

   @Override
   public void prepareKoPeMeExecution(final File logFile) {
      super.prepareKoPeMeExecution(logFile);

      adbPush();
      compileSources();
   }

   private void compileSources() {
      String wrapper = new File(folders.getProjectFolder(), EnvironmentVariables.fetchGradleCall()).getAbsolutePath();

      ProcessBuilder builder = new ProcessBuilder(wrapper, "installDebug", "installDebugAndroidTest");
      builder.directory(folders.getProjectFolder());

      try {
         Process process = builder.start();
         StreamGobbler.showFullProcess(process);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Executes the adb push command. 
    * Copies config files necessary for proper execution of KoPeMe
    */
   private void adbPush() {
      String adb = EnvironmentVariables.fetchAdbCall();

      // cleanup previous iteration
      removeDirInEmulator(adb, ANBOX_EMULATOR_FOLDER_BASE);
      createDirInEmulator(adb, ANBOX_EMULATOR_FOLDER_BASE);

      // copy files
      final String[] filesToBePushed = {
            "build.gradle",
            "gradle.properties",
            "app/build.gradle"
      };

      final File sourceFolder = folders.getProjectFolder();

      for (final String fileName : filesToBePushed) {
         File f = new File(fileName);

         pushSingleFile(adb, sourceFolder, fileName, f);
      }
   }

   private void pushSingleFile(String adb, final File sourceFolder, final String fileName, File f) {
      // create directories if necessary
      if (f.getParent() != null) {
         String subDir = String.format("%s%s", ANBOX_EMULATOR_FOLDER_BASE, f.getParent());
         createDirInEmulator(adb, subDir);
      }

      // push the file
      String destinationFolder = ANBOX_EMULATOR_FOLDER_BASE + (f.getParent() != null ? (f.getParent() + "/") : "");

      ProcessBuilder builder = new ProcessBuilder(adb, "push", fileName, destinationFolder);
      builder.directory(sourceFolder);
      LOG.debug("ADB: Pushing {}/{} to {}", sourceFolder, fileName, destinationFolder);

      try {
         Process process = builder.start();
         StreamGobbler.showFullProcess(process);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private void removeDirInEmulator(String adb, String path) {
      String shellCommand = String.format("rm -fr %s", path);
      ProcessBuilder builder = new ProcessBuilder(adb, "shell", shellCommand);
      LOG.debug("ADB: Removing directory {}", path);

      try {
         Process process = builder.start();
         StreamGobbler.showFullProcess(process);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
   
   private void createDirInEmulator(String adb, String path) {
      String shellCommand = String.format("mkdir -p %s", path);
      ProcessBuilder builder = new ProcessBuilder(adb, "shell", shellCommand);
      LOG.debug("ADB: Creating directory {}", path);

      try {
         Process process = builder.start();
         StreamGobbler.showFullProcess(process);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Executes the adb pull command. 
    * Copies measurementsTemp folder from emulator to the temporary project folder.
    */
    private void adbPull() {
      String adb = EnvironmentVariables.fetchAdbCall();
      
      ProcessBuilder builder = new ProcessBuilder(adb, "pull", ANBOX_EMULATOR_FOLDER_TEMP_RESULT, ".");
      builder.directory(folders.getPeassFolder());
      LOG.debug("ADB: Pulling {} to {}", ANBOX_EMULATOR_FOLDER_TEMP_RESULT, folders.getPeassFolder());
      try {
         Process process = builder.start();
         StreamGobbler.showFullProcess(process);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Executes the Gradle process; since gradle is run inside the module folder, different parameters than for the maven execution are required
    */
   private Process buildGradleProcess(final File moduleFolder, final File logFile, TestMethodCall test) {

      String[] anboxOriginals = new String[] { EnvironmentVariables.fetchAdbCall(), "shell", "am", "instrument", "-w", "-e", "class" };

      final String[] vars = CommandConcatenator.concatenateCommandArrays(anboxOriginals,
            new String[] { test.getExecutable(), test.getPackage() + ".test" + "/androidx.test.runner.AndroidJUnitRunner" });
      ProcessBuilderHelper processBuilderHelper = new ProcessBuilderHelper(env, folders);
      processBuilderHelper.parseParams(test.getParams());

      LOG.debug("Executing gradle test in moduleFolder: {}", moduleFolder);
      return processBuilderHelper.buildFolderProcess(moduleFolder, logFile, vars);
   }

   /**
    * Runs the given test and saves the results to the result folder.
    * 
    * @param specialResultFolder Folder for saving the results
    * @param testname Name of the test that should be run
    */
   @Override
   protected void runTest(final File moduleFolder, final File logFile, TestMethodCall test, final String testname, final long timeout) {
      final Process process = buildGradleProcess(moduleFolder, logFile, test);
      execute(testname, timeout, process);
      adbPull();
   }

}
