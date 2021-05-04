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
package de.dagere.peass.dependency.execution;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.execution.maven.MavenCleaner;
import de.dagere.peass.execution.maven.MavenRunningTester;
import de.dagere.peass.execution.maven.MavenUpdater;
import de.dagere.peass.execution.maven.PomPreparer;
import de.dagere.peass.execution.processutils.ProcessBuilderHelper;
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
   public static final String SUREFIRE_VERSION = "3.0.0-M5";
   public static final String DEFAULT_JAVA_VERSION = "1.8";

   public static final String KIEKER_ADAPTIVE_FILENAME = "config" + File.separator + "kieker.adaptiveMonitoring.conf";
   public static final File KIEKER_ASPECTJ_JAR = new File(ArgLineBuilder.KIEKER_FOLDER_MAVEN.replace("${user.home}", System.getProperty("user.home")));

   protected Charset lastEncoding = StandardCharsets.UTF_8;

   public MavenTestExecutor(final PeASSFolders folders, final JUnitTestTransformer testTransformer, final EnvironmentVariables env) {
      super(folders, testTransformer, env);
   }

   /**
    * Runs all tests and saves the results to the given result folder
    * 
    * @param specialResultFolder Folder for saving the results
    * @param tests Name of the test that should be run
    */
   @Override
   public void executeAllKoPeMeTests(final File logFile) {
      try {
         prepareKoPeMeExecution(logFile);
         final List<TestCase> testCases = getTestCases();
         LOG.info("Starting Testcases: {}", testCases.size());
         for (final TestCase test : testCases) {
            executeTest(test, logFile.getParentFile(), testTransformer.getConfig().getTimeoutInMinutes());
         }
      } catch (final XmlPullParserException | IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   public Process buildMavenProcess(final File logFile, final String... commandLineAddition) throws IOException, XmlPullParserException, InterruptedException {
      final String testGoal = getTestGoal();
      String mvnCall = env.fetchMavenCall();
      final String[] originals = new String[] { mvnCall,
            testGoal,
            "-fn",
            "-Djava.io.tmpdir=" + folders.getTempDir().getAbsolutePath() };
      String[] withMavendefaults = CommandConcatenator.concatenateCommandArrays(originals, CommandConcatenator.mavenCheckDeactivation);
      final String[] vars = CommandConcatenator.concatenateCommandArrays(withMavendefaults, commandLineAddition);

      ProcessBuilderHelper builder = new ProcessBuilderHelper(env, folders);
      Process process;
      if (testTransformer.getConfig().getExecutionConfig().getPl() != null) {
         String[] projectListArray = new String[] { "-pl", testTransformer.getConfig().getExecutionConfig().getPl(), "-am" };
         String[] withPl = CommandConcatenator.concatenateCommandArrays(vars, projectListArray);
         process = builder.buildFolderProcess(folders.getProjectFolder(), logFile, withPl);
      } else {
         process = builder.buildFolderProcess(folders.getProjectFolder(), logFile, vars);
      }
      return process;
   }

   @Override
   protected void clean(final File logFile) throws IOException, InterruptedException {
      new MavenCleaner(folders, env).clean(logFile);
   }

   @Override
   public void prepareKoPeMeExecution(final File logFile) throws IOException, InterruptedException, XmlPullParserException {
      updateJava();

      clean(logFile);
      LOG.debug("Starting Test Transformation");
      prepareKiekerSource();
      transformTests();

      PomPreparer pomPreparer = new PomPreparer(testTransformer, getModules(), folders);
      pomPreparer.preparePom();
      lastEncoding = pomPreparer.getLastEncoding();
   }

   private void updateJava() throws FileNotFoundException, IOException, XmlPullParserException {
      new MavenUpdater(folders, getModules(), testTransformer.getConfig()).updateJava();
      final File pomFile = new File(folders.getProjectFolder(), "pom.xml");
      if (testTransformer.getConfig().isRemoveSnapshots()) {
         MavenPomUtil.cleanSnapshotDependencies(pomFile);
      }
      PomJavaUpdater.fixCompilerVersion(pomFile);
      for (File module : getModules().getModules()) {
         final File pomFileModule = new File(module, "pom.xml");
         if (testTransformer.getConfig().isRemoveSnapshots()) {
            MavenPomUtil.cleanSnapshotDependencies(pomFileModule);
         }
         PomJavaUpdater.fixCompilerVersion(pomFileModule);
      }
   }

   @Override
   public void executeTest(final TestCase test, final File logFolder, final long timeout) {
      final File moduleFolder = new File(folders.getProjectFolder(), test.getModule());
      runMethod(logFolder, test, moduleFolder, timeout);
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
         final Process process = buildMavenProcess(logFile, "-Dtest=" + testname);
         execute(testname, timeout, process);
      } catch (final InterruptedException | IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   @Override
   public boolean isVersionRunning(final String version) {
      try {
         MavenRunningTester mavenRunningTester = new MavenRunningTester(folders, env, testTransformer.getConfig(), getModules());
         boolean isRunning = mavenRunningTester.isVersionRunning(version);
         buildfileExists = mavenRunningTester.isBuildfileExists();
         return isRunning;
      } catch (IOException | XmlPullParserException e) {
         throw new RuntimeException(e);
      }
   }

   public Charset getEncoding() {
      return lastEncoding;
   }

   @Override
   public ProjectModules getModules() throws IOException, XmlPullParserException {
      File pomFile = new File(folders.getProjectFolder(), "pom.xml");
      return MavenPomUtil.getModules(pomFile);
   }

}
