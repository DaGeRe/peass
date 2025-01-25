/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.dagere.peass.execution.maven.pom;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.execution.kieker.ArgLineBuilder;
import de.dagere.peass.execution.maven.AllModulePomPreparer;
import de.dagere.peass.execution.maven.MavenCleaner;
import de.dagere.peass.execution.maven.MavenRunningTester;
import de.dagere.peass.execution.maven.MavenUpdater;
import de.dagere.peass.execution.processutils.ProcessBuilderHelper;
import de.dagere.peass.execution.utils.CommandConcatenator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.KoPeMeExecutor;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

/**
 * Organizes the running of tests in a maven project by enhancing the pom, changing the test classes and calling the maven test goal
 * 
 * @author reichelt
 *
 */
public class MavenTestExecutor extends KoPeMeExecutor {

   private static final Logger LOG = LogManager.getLogger(MavenTestExecutor.class);

   /** M5 has some problems finding JUnit 5 tests; so stay at M3 */
   public static final String SUREFIRE_VERSION = "3.5.2";
   public static final String DEFAULT_JAVA_VERSION = "1.8";

   public static final String KIEKER_ADAPTIVE_FILENAME = "config" + File.separator + "kieker.adaptiveMonitoring.conf";
   public static final File KIEKER_ASPECTJ_JAR = new File(ArgLineBuilder.KIEKER_FOLDER_MAVEN.replace("${user.home}", System.getProperty("user.home")));

   protected Charset lastEncoding = StandardCharsets.UTF_8;

   public MavenTestExecutor(final PeassFolders folders, final JUnitTestTransformer testTransformer, final EnvironmentVariables env) {
      super(folders, testTransformer, env);
   }

   protected Process buildMavenProcess(final File logFile, TestMethodCall test, final String... commandLineAddition) throws IOException, InterruptedException {
      final String testGoal = getTestGoal();
      String mvnCall = env.fetchMavenCall(getProjectFolder());
      final String[] originals = new String[] { mvnCall,
            "--batch-mode",
            testGoal,
            "-fn",
            ArgLineBuilder.TEMP_DIR + "=" + folders.getTempDir().getAbsolutePath() };
      String[] withMavendefaults = CommandConcatenator.concatenateCommandArrays(originals, CommandConcatenator.mavenCheckDeactivation);
      final String[] vars = CommandConcatenator.concatenateCommandArrays(withMavendefaults, commandLineAddition);

      ProcessBuilderHelper processBuilderHelper = new ProcessBuilderHelper(env, folders);
      processBuilderHelper.parseParams(test.getParams());

      String[] withPl = addMavenPl(testTransformer.getConfig().getExecutionConfig(), vars);
      Process process = processBuilderHelper.buildFolderProcess(folders.getProjectFolder(), logFile, withPl);
      return process;
   }

   @Override
   protected void clean(final File logFile) {
      new MavenCleaner(folders, env).clean(logFile);
   }

   @Override
   public void prepareKoPeMeExecution(final File logFile) {
      updateJava();

      clean(logFile);
      LOG.debug("Starting Test Transformation");
      prepareKiekerSource();
      transformTests();

      AllModulePomPreparer pomPreparer = new AllModulePomPreparer(testTransformer, getModules(), folders);
      pomPreparer.preparePom();
      lastEncoding = pomPreparer.getLastEncoding();
   }

   private void updateJava() {
      try {
         new MavenUpdater(folders, getModules(), testTransformer.getConfig()).updateJava();
         final File pomFile = new File(folders.getProjectFolder(), "pom.xml");
         LOG.info("Remove snapshots: " + testTransformer.getConfig().getExecutionConfig().isRemoveSnapshots());
         if (testTransformer.getConfig().getExecutionConfig().isRemoveSnapshots()) {
            SnapshotRemoveUtil.cleanSnapshotDependencies(pomFile);
         }
         PomJavaUpdater.fixCompilerVersion(pomFile);
         for (File module : getModules().getModules()) {
            final File pomFileModule = new File(module, "pom.xml");
            if (testTransformer.getConfig().getExecutionConfig().isRemoveSnapshots()) {
               SnapshotRemoveUtil.cleanSnapshotDependencies(pomFileModule);
            }
            PomJavaUpdater.fixCompilerVersion(pomFileModule);
         }
      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }

   }

   @Override
   public void executeTest(final TestMethodCall test, final File logFolder, final long timeout) {
      final File moduleFolder = new File(folders.getProjectFolder(), test.getModule());
      runMethod(logFolder, test, moduleFolder, timeout);
      
      cleanAboveSize(logFolder, "txt");
   }

   @Override
   public void executeTest(final String javaAgent, final TestMethodCall test, final File logFolder, final long timeout) {
      final File moduleFolder = new File(folders.getProjectFolder(), test.getModule());
      runMethod(logFolder, test, moduleFolder, timeout, javaAgent);
      
      cleanAboveSize(logFolder, "txt");
   }

   /**
    * Runs the given test and saves the results to the result folder.
    * 
    * @param specialResultFolder Folder for saving the results
    * @param testname Name of the test that should be run
    */
   @Override
   protected void runTest(final String javaAgent, final File module, final File logFile, TestMethodCall test, final String testname, final long timeout) {
      try {
         final Process process = buildMavenProcess(logFile, test, "-Dtest=" + testname, javaAgent);
         execute(testname, timeout, process);
      } catch (final InterruptedException | IOException e) {
         e.printStackTrace();
      }
   }

   /**
    * Runs the given test and saves the results to the result folder.
    * 
    * @param specialResultFolder Folder for saving the results
    * @param testname Name of the test that should be run
    */
    @Override
    protected void runTest(final File module, final File logFile, TestMethodCall test, final String testname, final long timeout) {
       try {
          final Process process = buildMavenProcess(logFile, test, "-Dtest=" + testname);
          execute(testname, timeout, process);
       } catch (final InterruptedException | IOException e) {
          e.printStackTrace();
       }
    }

   @Override
   public boolean doesBuildfileExist() {
      File pomFile = new File(folders.getProjectFolder(), "pom.xml");
      boolean buildfileExists = pomFile.exists();
      return buildfileExists;
   }

   @Override
   public boolean isCommitRunning(final String commit) {
      ProjectModules modules = getModules();
      if (modules != null) {
         MavenRunningTester mavenRunningTester = new MavenRunningTester(folders, testTransformer.getConfig(), env, modules);
         boolean isRunning = mavenRunningTester.isCommitRunning(commit, this);
         return isRunning;
      } else {
         return false;
      }

   }

   public Charset getEncoding() {
      return lastEncoding;
   }

   @Override
   public ProjectModules getModules() {
      File pomFile = new File(folders.getProjectFolder(), "pom.xml");
      return MavenPomUtil.getModules(pomFile, testTransformer.getConfig().getExecutionConfig());
   }

   public static String[] addMavenPl(final ExecutionConfig config, final String[] original) {
      if (config.getPl() != null) {
         String[] projectListArray = new String[] { "-pl", config.getPl(), "-am" };
         String[] withPl = CommandConcatenator.concatenateCommandArrays(original, projectListArray);
         return withPl;
      } else {
         return original;
      }
   }

}
