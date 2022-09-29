package de.dagere.peass.execution.gradle;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.execution.processutils.ProcessBuilderHelper;
import de.dagere.peass.execution.utils.CommandConcatenator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.utils.StreamGobbler;

public class AnboxTestExecutor extends GradleTestExecutor {

   private static final Logger LOG = LogManager.getLogger(AnboxTestExecutor.class);

   public AnboxTestExecutor(final PeassFolders folders, final JUnitTestTransformer testTransformer, final EnvironmentVariables env) {
      super(folders, testTransformer, env);
   }

   @Override
   public void prepareKoPeMeExecution(final File logFile) {
      super.prepareKoPeMeExecution(logFile);

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
    * Executes the adb pull command. 
    * Copies measurementsTemp folder from emulator to the temporary project folder.
    */
    private void adbPull() {
      String adb = EnvironmentVariables.fetchAdbCall();
      String androidTempResultFolder = "/storage/emulated/0/Documents/measurementsTemp"; // temporary solution

      ProcessBuilder builder = new ProcessBuilder(adb, "pull", androidTempResultFolder, ".");
      builder.directory(folders.getPeassFolder());
      LOG.debug("ADB: Pulling {} to {}", androidTempResultFolder, folders.getPeassFolder());
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

      String[] anboxOriginals = new String[] { "adb", "shell", "am", "instrument", "-w", "-e", "class" };

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
