package de.dagere.peass.execution.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.parsing.GradleParseHelper;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.execution.maven.MavenRunningTester;
import de.dagere.peass.execution.processutils.ProcessBuilderHelper;
import de.dagere.peass.execution.processutils.ProcessSuccessTester;
import de.dagere.peass.execution.utils.CommandConcatenator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.KoPeMeExecutor;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class GradleTestExecutor extends KoPeMeExecutor {

   private static final Logger LOG = LogManager.getLogger(GradleTestExecutor.class);

   protected final File gradleHome;

   public GradleTestExecutor(final PeassFolders folders, final JUnitTestTransformer testTransformer, final EnvironmentVariables env) {
      super(folders, testTransformer, env);

      this.gradleHome = getGradleHome();
      env.getEnvironmentVariables().put("GRADLE_HOME", gradleHome.getAbsolutePath());
   }

   @Override
   public void prepareKoPeMeExecution(final File logFile) {
      LOG.debug("Starting Test Transformation");
      prepareKiekerSource();
      transformTests();

      prepareBuildfile();

   }

   public File getGradleHome() {
      File gradleHome;
      File projectFolder = folders.getProjectFolder();
      final File peassFolder = new File(projectFolder.getParentFile(), projectFolder.getName() + PeassFolders.PEASS_POSTFIX);
      gradleHome = new File(peassFolder, "gradleHome");
      gradleHome.mkdir();
      final File init = new File(gradleHome, "init.gradle");
      GradleParseUtil.writeInitGradle(init);
      return gradleHome;
   }

   private void prepareBuildfile() {
      try {
         lastTmpFile = Files.createTempDirectory(folders.getKiekerTempFolder().toPath(), "kiekerTemp").toFile();
         isAndroid = false;
         ProjectModules modules = getModules();
         LOG.debug("Preparing modules: {}", modules);
         replaceAllBuildfiles(modules);
         for (final File module : modules.getModules()) {
            final File gradleFile = GradleParseHelper.findGradleFile(module);
            editOneBuildfile(gradleFile, modules);
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   private void replaceBuildfile(final File gradleFile) throws IOException {
      File potentialAlternativeFile = new File(gradleFile.getParentFile(), GradleParseHelper.ALTERNATIVE_NAME);
      if (potentialAlternativeFile.exists()) {
         LOG.debug("Replacing {} by {}", gradleFile, potentialAlternativeFile);
         gradleFile.delete();
         FileUtils.moveFile(potentialAlternativeFile, gradleFile);
      }
   }

   private void editOneBuildfile(final File gradleFile, final ProjectModules modules) {
      final GradleBuildfileVisitor visitor;
      GradleBuildfileEditor editor = new GradleBuildfileEditor(testTransformer, gradleFile, modules);
      visitor = editor.addDependencies(lastTmpFile, env);
      if (visitor.isAndroid()) {
         isAndroid = true;
      }
   }

   protected String getCleanGoal() {
      String cleanGoal;
      ExecutionConfig executionConfig = testTransformer.getConfig().getExecutionConfig();
      cleanGoal = executionConfig.getCleanGoal() != null ? executionConfig.getCleanGoal() : "clean";
      return cleanGoal;
   }

   /**
    * Executes the Gradle process; since gradle is run inside the module folder, different parameters than for the maven execution are required
    */
   private Process buildGradleProcess(final File moduleFolder, final File logFile, TestMethodCall test, final String... commandLineAddition)
         throws IOException {
      final String testGoal = getTestGoal();
      String wrapper = new File(folders.getProjectFolder(), EnvironmentVariables.fetchGradleCall()).getAbsolutePath();
      String[] originals = new String[] { wrapper,
            "--init-script", new File(gradleHome, "init.gradle").getAbsolutePath(),
            "--no-daemon",
            getCleanGoal(), testGoal };
      LOG.debug("Redirecting to null: {}", testTransformer.getConfig().getExecutionConfig().isRedirectToNull());
      if (!testTransformer.getConfig().getExecutionConfig().isRedirectToNull()) {
         originals = CommandConcatenator.concatenateCommandArrays(originals, new String[] { "--info" });
      }

      final String[] vars = CommandConcatenator.concatenateCommandArrays(originals, commandLineAddition);
      ProcessBuilderHelper processBuilderHelper = new ProcessBuilderHelper(env, folders);
      processBuilderHelper.parseParams(test.getParams());

      LOG.debug("Executing gradle test in moduleFolder: {}", moduleFolder);
      return processBuilderHelper.buildFolderProcess(moduleFolder, logFile, vars);
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
   public void executeTest(final TestMethodCall test, final File logFolder, final long timeout) {
      final File module = new File(folders.getProjectFolder(), test.getModule());
      cleanLastTest(module);
      runMethod(logFolder, test, module, timeout);

      cleanAboveSize(logFolder, "txt");
   }

   @Override
   public void executeTest(String javaAgent, final TestMethodCall test, final File logFolder, final long timeout) {
      throw new RuntimeException("Functionality regarding running tests with javaagent on Gradle is not currently supported.");
   }

   /**
    * Runs the given test and saves the results to the result folder.
    * 
    * @param specialResultFolder Folder for saving the results
    * @param testname Name of the test that should be run
    */
    @Override
    protected void runTest(String javaAgent, final File moduleFolder, final File methodLogFile, TestMethodCall test, final String testname, final long timeout) {
       throw new RuntimeException("Functionality regarding running tests with javaagent on Gradle is not currently supported.");
    }

   /**
    * Runs the given test and saves the results to the result folder.
    * 
    * @param specialResultFolder Folder for saving the results
    * @param testname Name of the test that should be run
    */
   @Override
   protected void runTest(final File moduleFolder, final File methodLogFile, TestMethodCall test, final String testname, final long timeout) {
      try {
         final Process process = buildGradleProcess(moduleFolder, methodLogFile, test, "--tests", testname);
         execute(testname, timeout, process);

         GradleDaemonFileDeleter.deleteDaemonFile(methodLogFile);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   public boolean doesBuildfileExist() {
      final File wrapper = new File(folders.getProjectFolder(), EnvironmentVariables.fetchGradleCall());
      final File potentialBuildfile = new File(folders.getProjectFolder(), "build.gradle");
      boolean buildfileExists = wrapper.exists() && potentialBuildfile.exists();
      return buildfileExists;
   }

   @Override
   public boolean isCommitRunning(final String commit) {
      ProjectModules modules = getModules();
      if (modules != null) {
         GradleRunningTester mavenRunningTester = new GradleRunningTester(folders, testTransformer.getConfig(), env, modules);
         boolean isRunning = mavenRunningTester.isCommitRunning(commit, this);
         isAndroid = mavenRunningTester.isAndroid();
         return isRunning;
      } else {
         return false;
      }
   }

   void replaceAllBuildfiles(final ProjectModules modules) {
      if (testTransformer.getConfig().getExecutionConfig().isUseAlternativeBuildfile()) {
         for (final File module : modules.getModules()) {
            final File gradleFile = GradleParseHelper.findGradleFile(module);
            try {
               replaceBuildfile(gradleFile);
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      }
   }

   @Override
   public ProjectModules getModules() {
      return SettingsFileParser.getModules(folders.getProjectFolder());
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
