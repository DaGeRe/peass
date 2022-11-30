package de.dagere.peass.execution.gradle;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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

      updateAndroidManifest();
      adbPush();      
      compileSources();
   }

   private void updateAndroidManifest() {
      File projectFolder = folders.getProjectFolder();

      // TODO make the AndroidManifest.xml path configurable
      File manifestFile = new File(projectFolder, "app/src/main/AndroidManifest.xml");

      try {
         // parse XML file to build DOM
         DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
         Document dom = builder.parse(manifestFile);
     
         // normalize XML structure
         dom.normalizeDocument();
     
         // get root element
         Element root = dom.getDocumentElement();

         Node nodeApplication = root.getElementsByTagName("application").item(0);

         if (nodeApplication != null && nodeApplication.getNodeType() == Node.ELEMENT_NODE) {
            ((Element) nodeApplication).setAttribute("android:requestLegacyExternalStorage", "true");
         }

         // add read external storage permission element
         Element elementUsesPermissionReadExternal = dom.createElement("uses-permission");
         elementUsesPermissionReadExternal.setAttribute("android:name", "android.permission.READ_EXTERNAL_STORAGE");
         root.insertBefore(elementUsesPermissionReadExternal, nodeApplication); // if nodeApplication is null, appended to the end

         // add write external storage permission element
         Element elementUsesPermissionWriteExternal = dom.createElement("uses-permission");
         elementUsesPermissionWriteExternal.setAttribute("android:name", "android.permission.WRITE_EXTERNAL_STORAGE");
         root.insertBefore(elementUsesPermissionWriteExternal, nodeApplication); // if nodeApplication is null, appended to the end

         // write back
         Transformer transformer = TransformerFactory.newInstance().newTransformer();
         
         transformer.setOutputProperty(OutputKeys.METHOD, "xml");
         transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
         transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
         transformer.setOutputProperty(OutputKeys.INDENT, "yes");

         transformer.transform(new DOMSource(dom), new StreamResult(manifestFile));
      } catch (Exception ex) {
         ex.printStackTrace();
      }
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
