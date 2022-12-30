package de.dagere.peass.execution.gradle;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.execution.processutils.ProcessBuilderHelper;
import de.dagere.peass.execution.utils.CommandConcatenator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.utils.Constants;
import de.dagere.peass.utils.StreamGobbler;

public class AnboxTestExecutor extends GradleTestExecutor {

   public static final String ANBOX_EMULATOR_FOLDER_BASE = "/storage/emulated/0/Documents/peass/";
   public static final String ANBOX_EMULATOR_FOLDER_TEMP_RESULT = ANBOX_EMULATOR_FOLDER_BASE + "measurementsTemp/";
   public static final String ANDROID_RESOURCES_FOLDER = "app/src/main/resources/";
   public static final String ANDROID_KOPEME_CONFIGURATION = "kopeme_config.json";

   private static final Logger LOG = LogManager.getLogger(AnboxTestExecutor.class);

   private final String adbCall;
   private final AnboxDirHandler dirHandler;

   public AnboxTestExecutor(final PeassFolders folders, final JUnitTestTransformer testTransformer, final EnvironmentVariables env) {
      super(folders, testTransformer, env);
      this.adbCall = EnvironmentVariables.fetchAdbCall();
      dirHandler = new AnboxDirHandler(adbCall);
   }

   @Override
   public void prepareKoPeMeExecution(final File logFile) {
      super.prepareKoPeMeExecution(logFile);

      writeAndroidConfigJson();
      updateAndroidManifest();
      adbPush();      
      compileSources();
   }

   /**
    * Writes Android configuration file inside "app/src/main/resources" to pass values for KoPeMe inside the emulator.
    */
   private void writeAndroidConfigJson() {
      ObjectNode androidConfig = Constants.OBJECTMAPPER.createObjectNode();
      androidConfig.put("KOPEME_HOME", ANBOX_EMULATOR_FOLDER_TEMP_RESULT);
      // TODO: Change the hard coded module name 'app'.
      androidConfig.put("kopeme.workingdir", ANBOX_EMULATOR_FOLDER_BASE + "app");
      File kopemeConfig = new File(folders.getProjectFolder() + "/" + ANDROID_RESOURCES_FOLDER + ANDROID_KOPEME_CONFIGURATION);
      try {
         kopemeConfig.getParentFile().mkdirs();
         Constants.OBJECTMAPPER.writerWithDefaultPrettyPrinter().writeValue(kopemeConfig, androidConfig);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private void updateAndroidManifest() {
      File projectFolder = folders.getProjectFolder();

      String relativeManifestFilePath = testTransformer.getConfig().getExecutionConfig().getAndroidManifest();

      if (relativeManifestFilePath != null) {
         File manifestFile = new File(projectFolder, relativeManifestFilePath);

         try {
            ManifestEditor manifestEditor = new ManifestEditor(manifestFile);
            manifestEditor.updateForExternalStorageReadWrite();
         } catch (Exception ex) {
            ex.printStackTrace();
         }
      } else {
         LOG.error("Android manifest file path is not specified. Use --androidManifest switch to specify the path.");
      }
   }

   private void compileSources() {
      String wrapper = new File(folders.getProjectFolder(), EnvironmentVariables.fetchGradleCall()).getAbsolutePath();
      List<String> gradleTasks = testTransformer.getConfig().getExecutionConfig().getAndroidGradleTasks();

      String[] processArgs = toMergedArray(wrapper, gradleTasks);

      ProcessBuilder builder = new ProcessBuilder(processArgs);
      builder.directory(folders.getProjectFolder());
      for (Map.Entry<String, String> entry : env.getEnvironmentVariables().entrySet()) {
         LOG.trace("Environment: {} = {}", entry.getKey(), entry.getValue());
         builder.environment().put(entry.getKey(), entry.getValue());
      }

      try {
         Process process = builder.start();
         StreamGobbler.showFullProcess(process);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   private String[] toMergedArray(String first, List<String> remaining)
   {
      String[] mergedArray = new String[1 + (remaining != null ? remaining.size() : 0)];
      mergedArray[0] = first;

      if (remaining != null) {
         int index = 1;

         for (String element : remaining) {
            mergedArray[index++] = element;
         }
      }

      return mergedArray;
   }

   /**
    * Executes the adb push command. Copies config files necessary for proper execution of KoPeMe
    */
   private void adbPush() {
      // cleanup previous iteration
      dirHandler.removeDirInEmulator(ANBOX_EMULATOR_FOLDER_BASE);
      dirHandler.createDirInEmulator(ANBOX_EMULATOR_FOLDER_BASE);

      // copy files
      final String[] filesToBePushed = {
            "build.gradle",
            "gradle.properties",
            "app/build.gradle"
      };

      final File sourceFolder = folders.getProjectFolder();

      for (final String fileName : filesToBePushed) {
         File f = new File(fileName);

         pushSingleFile(sourceFolder, fileName, f);
      }
   }

   private void pushSingleFile(final File sourceFolder, final String fileName, File f) {
      // create directories if necessary
      if (f.getParent() != null) {
         String subDir = String.format("%s%s", ANBOX_EMULATOR_FOLDER_BASE, f.getParent());
         dirHandler.createDirInEmulator(subDir);
      }

      // push the file
      String destinationFolder = ANBOX_EMULATOR_FOLDER_BASE + (f.getParent() != null ? (f.getParent() + "/") : "");

      ProcessBuilder builder = new ProcessBuilder(adbCall, "push", fileName, destinationFolder);
      builder.directory(sourceFolder);
      LOG.debug("ADB: Pushing {}/{} to {}", sourceFolder, fileName, destinationFolder);

      try {
         Process process = builder.start();
         StreamGobbler.showFullProcess(process);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Executes the adb pull command. Copies measurementsTemp folder from emulator to the temporary project folder.
    */
   private void adbPull() {
      ProcessBuilder builder = new ProcessBuilder(adbCall, "pull", ANBOX_EMULATOR_FOLDER_TEMP_RESULT, ".");
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

      String[] anboxOriginals = new String[] { adbCall, "shell", "am", "instrument", "-w", "-e", "class" };

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
