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
package de.peass.dependency.execution;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.PeASSFolders;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.testtransformation.JUnitTestTransformer;

/**
 * Organizes the running of tests in a maven project by enhancing the pom, changing the test classes and calling the maven test goal
 * 
 * @author reichelt
 *
 */
public class MavenTestExecutor extends TestExecutor {

   private static final Logger LOG = LogManager.getLogger(MavenTestExecutor.class);

   /** M5 has some problems finding JUnit 5 tests; so stay at M3 */
   public static final String SUREFIRE_VERSION = "3.0.0-M3";
   public static final String DEFAULT_JAVA_VERSION = "1.8";

   public static final String KIEKER_ADAPTIVE_FILENAME = "config/kieker.adaptiveMonitoring.conf";
   public static final File KIEKER_ASPECTJ_JAR = new File(ArgLineBuilder.KIEKER_FOLDER_MAVEN.replace("${user.home}", System.getProperty("user.home")));

   protected Charset lastEncoding = StandardCharsets.UTF_8;

   public MavenTestExecutor(final PeASSFolders folders, final JUnitTestTransformer testTransformer) {
      super(folders, testTransformer);
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

   protected Process buildMavenProcess(final File logFile, final String... commandLineAddition) throws IOException, XmlPullParserException, InterruptedException {
      final String testGoal = getTestGoal();
      final String[] originals = new String[] { "mvn",
            testGoal,
            "-fn",
            "-Dcheckstyle.skip=true",
            // "-Dmaven.compiler.source=" + JAVA_VERSION,
            // "-Dmaven.compiler.target=" + JAVA_VERSION,
            "-Dmaven.javadoc.skip=true",
            "-Danimal.sniffer.skip=true",
            "-Denforcer.skip=true",
            "-DfailIfNoTests=false",
            "-Drat.skip=true",
            "-Djacoco.skip=true",
            "-Djava.io.tmpdir=" + folders.getTempDir().getAbsolutePath() };

      final String[] vars = new String[commandLineAddition.length + originals.length];
      for (int i = 0; i < originals.length; i++) {
         vars[i] = originals[i];
      }
      for (int i = 0; i < commandLineAddition.length; i++) {
         vars[originals.length + i] = commandLineAddition[i];
      }

      return buildFolderProcess(folders.getProjectFolder(), logFile, vars);
   }

   @Override
   protected void clean(final File logFile) throws IOException, InterruptedException {
      final String[] originalsClean = new String[] { "mvn", "clean" };
      final ProcessBuilder pbClean = new ProcessBuilder(originalsClean);
      pbClean.directory(folders.getProjectFolder());
      if (logFile != null) {
         pbClean.redirectOutput(Redirect.appendTo(logFile));
         pbClean.redirectError(Redirect.appendTo(logFile));
      }

      cleanSafely(pbClean);
   }

   private void cleanSafely(final ProcessBuilder pbClean) throws IOException, InterruptedException {
      boolean finished = false;
      int count = 0;
      while (!finished && count < 10) {
         final Process processClean = pbClean.start();
         finished = processClean.waitFor(60, TimeUnit.MINUTES);
         if (!finished) {
            LOG.info("Clean process " + processClean + " was not finished successfully; trying again to clean");
            processClean.destroyForcibly();
         }
         count++;
      }
   }

   @Override
   public void prepareKoPeMeExecution(final File logFile) throws IOException, InterruptedException, XmlPullParserException {
      updateJava();

      clean(logFile);
      LOG.debug("Starting Test Transformation");
      prepareKiekerSource();
      transformTests();

      preparePom();
   }

   private void updateJava() throws FileNotFoundException, IOException, XmlPullParserException {
      final File pomFile = new File(folders.getProjectFolder(), "pom.xml");
      if (testTransformer.getConfig().isRemoveSnapshots()) {
         MavenPomUtil.cleanSnapshotDependencies(pomFile);
      }
      PomJavaUpdater.fixCompilerVersion(pomFile);
      for (File module : getModules()) {
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
      final ChangedEntity testClazzEntity = new ChangedEntity(test.getClazz(), test.getModule());
      runMethod(logFolder, testClazzEntity, moduleFolder, test.getMethod(), timeout);
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
      File pomFile = new File(folders.getProjectFolder(), "pom.xml");
      final File potentialPom = pomFile;
      final File testFolder = new File(folders.getProjectFolder(), "src/test");
      final boolean isRunning = false;
      buildfileExists = potentialPom.exists();
      if (potentialPom.exists()) {
         try {
            final boolean multimodule = MavenPomUtil.isMultiModuleProject(potentialPom);
            if (multimodule || testFolder.exists()) {
               updateJava();
               MavenPomUtil.cleanType(pomFile);
               return testRunningSuccess(version,
                     new String[] { "mvn", "clean", "test-compile",
                           "-DskipTests=true",
                           "-Dmaven.test.skip.exec",
                           "-Dcheckstyle.skip=true",
                           // "-Dmaven.compiler.source=" + DEFAULT_JAVA_VERSION,
                           // "-Dmaven.compiler.target=" + DEFAULT_JAVA_VERSION,
                           "-Dmaven.javadoc.skip=true",
                           "-Danimal.sniffer.skip=true",
                           "-Djacoco.skip=true",
                           "-Drat.skip=true",
                           "-Denforcer.skip=true",
                           "-DfailIfNoTests=false" });
            } else {
               LOG.error("Expected src/test to exist");
               return false;
            }
         } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
         }
      } else {
         LOG.error("No pom.xml in {}", version);
      }
      return isRunning;
   }

   public void preparePom() {
      try {
         lastTmpFile = Files.createTempDirectory(folders.getKiekerTempFolder().toPath(), "kiekerTemp").toFile();
         for (final File module : getModules()) {
            editOneBuildfile(true, new File(module, "pom.xml"));
         }
      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   private void editOneBuildfile(final boolean update, final File pomFile) {
      try {
         final Model model;
         try (FileInputStream fileInputStream = new FileInputStream(pomFile)) {
            final MavenXpp3Reader reader = new MavenXpp3Reader();
            model = reader.read(fileInputStream);
         }

         if (model.getBuild() == null) {
            model.setBuild(new Build());
         }
         final String argline = new ArgLineBuilder(testTransformer).buildArgline(lastTmpFile);

         MavenPomUtil.extendSurefire(argline, model, update, testTransformer.getConfig().getTimeoutInMinutes() * 2);

         // TODO Move back to extend dependencies, if stable Kieker version supports <init>
         if (model.getDependencies() == null) {
            model.setDependencies(new LinkedList<Dependency>());
         }
         MavenPomUtil.extendDependencies(model, testTransformer.isJUnit3());

         try (FileWriter fileWriter = new FileWriter(pomFile)) {
            final MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(fileWriter, model);
         }

         lastEncoding = MavenPomUtil.getEncoding(model);
      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   public Charset getEncoding() {
      return lastEncoding;
   }

   @Override
   public List<File> getModules() throws IOException, XmlPullParserException {
      return MavenPomUtil.getModules(new File(folders.getProjectFolder(), "pom.xml"));
   }

}
