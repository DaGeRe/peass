package de.dagere.peass.dependency.execution;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.kopeme.parsing.GradleParseHelper;
import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.execution.gradle.FindDependencyVisitor;
import de.dagere.peass.execution.processutils.ProcessBuilderHelper;
import de.dagere.peass.execution.processutils.ProcessSuccessTester;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class GradleTestExecutor extends KoPeMeExecutor {

   private static final Logger LOG = LogManager.getLogger(GradleTestExecutor.class);

   public GradleTestExecutor(final PeASSFolders folders, final JUnitTestTransformer testTransformer, final EnvironmentVariables env) {
      super(folders, testTransformer, env);
   }

   @Override
   public void executeAllKoPeMeTests(final File logFile) throws IOException, XmlPullParserException, InterruptedException {
      prepareKoPeMeExecution(logFile);
      try {
         final int testcount = getTestCount();
         final Process process = buildProcess(folders.getProjectFolder(), logFile);
         LOG.info("Starting Process, tests: {}", testcount);
         final long timeout = 1l + testcount * this.testTransformer.getConfig().getTimeoutInMinutes();
         execute("all", timeout, process);
      } catch (InterruptedException | IOException | XmlPullParserException e) {
         e.printStackTrace();
      }

   }

   @Override
   public void prepareKoPeMeExecution(final File logFile) throws IOException, XmlPullParserException, InterruptedException {
      LOG.debug("Starting Test Transformation");
      prepareKiekerSource();
      transformTests();

      prepareBuildfile();

   }

   private void prepareBuildfile() {
      try {
         lastTmpFile = Files.createTempDirectory("kiekerTemp").toFile();
         isAndroid = false;
         ProjectModules modules = getModules();
         LOG.debug("Preparing modules: {}", modules);
         for (final File module : modules.getModules()) {
            final File gradleFile = GradleParseHelper.findGradleFile(module);
            editOneBuildfile(gradleFile, modules);
         }
      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   private void editOneBuildfile(final File gradleFile, final ProjectModules modules) {
      final FindDependencyVisitor visitor;
      GradleBuildfileEditor editor = new GradleBuildfileEditor(testTransformer, gradleFile, modules);
      if (testTransformer.getConfig().isUseKieker()) {
         visitor = editor.addDependencies(lastTmpFile);
      } else {
         visitor = editor.addDependencies(null);
      }
      if (visitor.isAndroid()) {
         isAndroid = true;
      }
   }

   protected Process buildProcess(final File folder, final File logFile, final String... commandLineAddition) throws IOException, XmlPullParserException, InterruptedException {
      final String testGoal = getTestGoal();
      final String[] originals = new String[] { new File(folders.getProjectFolder(), "gradlew").getAbsolutePath(),
            "--init-script", new File(folders.getGradleHome(), "init.gradle").getAbsolutePath(),
            "--no-daemon",
            "cleanTest", testGoal };

      final String[] vars = CommandConcatenator.concatenateCommandArrays(originals, commandLineAddition);
      ProcessBuilderHelper processBuilderHelper = new ProcessBuilderHelper(env, folders);
      return processBuilderHelper.buildFolderProcess(folder, logFile, vars);
   }

   /**
    * Since older gradle versions do not re-execute tests, the build-folder need to be cleared before test re-execution
    */
   public void cleanLastTest(final File module) {
      final File testFolder = new File(module, "build" + File.separator + "test-results" + File.separator + "release");
      if (testFolder.exists()) {
         LOG.debug("Cleaning: {}", testFolder);
         try {
            FileUtils.deleteDirectory(testFolder);
         } catch (final IOException e) {
            e.printStackTrace();
         }
      } else {
         LOG.debug("Cleaning not necessary: {}", testFolder);
      }
   }

   @Override
   public void executeTest(final TestCase test, final File logFolder, final long timeout) {
      final File module = new File(folders.getProjectFolder(), test.getModule());
      cleanLastTest(module);
      runMethod(logFolder, test, module, timeout);
   }

   /**
    * Runs the given test and saves the results to the result folder.
    * 
    * @param specialResultFolder Folder for saving the results
    * @param testname Name of the test that should be run
    */
   @Override
   protected void runTest(final File module, final File logFile, final String testname, final long timeout) {
      try {
         final Process process = buildProcess(module, logFile, "--tests", testname);
         execute(testname, timeout, process);
      } catch (final InterruptedException | IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   @Override
   public boolean isVersionRunning(final String version) {
      final File wrapper = new File(folders.getProjectFolder(), "gradlew");
      final File potentialBuildfile = new File(folders.getProjectFolder(), "build.gradle");
      boolean isRunning = false;
      buildfileExists = wrapper.exists() && potentialBuildfile.exists();
      if (buildfileExists) {
         try {
            boolean isAndroid = false;
            for (final File module : getModules().getModules()) {
               final File buildfile = GradleParseHelper.findGradleFile(module);
               final FindDependencyVisitor visitor = GradleParseUtil.setAndroidTools(buildfile);
               if (visitor.isAndroid()) {
                  isAndroid = true;
                  if (!visitor.hasVersion()) {
                     return false;
                  }
               }
            }
            this.isAndroid = isAndroid;
         } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
         }
         final String[] vars = new String[] { "./gradlew", "--no-daemon", "assemble" };
         isRunning = new ProcessSuccessTester(folders, testTransformer.getConfig(), env).testRunningSuccess(version, vars);
      }
      return isRunning;
   }

   @Override
   public ProjectModules getModules() throws IOException, XmlPullParserException {
      return GradleParseUtil.getModules(folders.getProjectFolder());
   }

   @Override
   public boolean isAndroid() {
      return isAndroid;
   }

   @Override
   protected void clean(final File logFile) throws IOException, InterruptedException {
      // TODO Auto-generated method stub
   }

}
