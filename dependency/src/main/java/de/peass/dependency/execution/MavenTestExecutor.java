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
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peass.dependency.PeASSFolders;
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

   public static final String SUREFIRE_VERSION = "2.21.0";
   public static final String JAVA_VERSION = "1.8";

   public static final String TEMP_DIR = "-Djava.io.tmpdir";
   public static final String JAVA_AGENT = "-javaagent";
   public static final String KIEKER_FOLDER_MAVEN = "${user.home}/.m2/repository/net/kieker-monitoring/kieker/1.13/kieker-1.13-aspectj.jar";
   public static final String KIEKER_FOLDER_GRADLE = "${System.properties['user.home']}/.m2/repository/net/kieker-monitoring/kieker/1.13/kieker-1.13-aspectj.jar";
   /**
    * This is added to surefire, assuming that kieker has been downloaded already, so that the aspectj-weaving can take place.
    */
   protected static final String KIEKER_ARG_LINE = JAVA_AGENT + ":" + KIEKER_FOLDER_MAVEN;

   protected Charset lastEncoding = StandardCharsets.UTF_8;

   public MavenTestExecutor(final PeASSFolders folders, final JUnitTestTransformer testTransformer, final long timeout) {
      super(folders, timeout, testTransformer);
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
         // final int testcount = getTestCount();
         // final Process process = buildProcess(logFile);

         // final long timeout = 1l + testcount * this.timeout;
         final List<TestCase> testCases = getTestCases();
         LOG.info("Starting Testcases: {}", testCases.size());
         for (final TestCase test : testCases) {
            executeTest(test, logFile.getParentFile(), timeout);
         }
         // execute("all", timeout, process);
         // process.waitFor();
      } catch (final XmlPullParserException | IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   protected void generateAOPXML() {
      try {
         for (final File module : getModules()) {
            for (final String potentialReadFolder : new String[] { "src/main/resources/META-INF", "src/java/META-INF", "src/test/resources/META-INF", "src/test/META-INF",
                  "target/test-classes/META-INF" }) {
               final File folder = new File(module, potentialReadFolder);
               folder.mkdirs();
               final File goalFile2 = new File(folder, "aop.xml");
               AOPXMLHelper.writeAOPXMLToFile(existingClasses, goalFile2);
            }
         }
      } catch (final XmlPullParserException | IOException e) {
         e.printStackTrace();
      }
   }

   protected Process buildProcess(final File logFile, final String... commandLineAddition) throws IOException, XmlPullParserException, InterruptedException {
      final String[] originals;
      originals = new String[] { "mvn",
            "test",
            "-fn",
            "-Dcheckstyle.skip=true",
            "-Dmaven.compiler.source=" + JAVA_VERSION,
            "-Dmaven.compiler.target=" + JAVA_VERSION,
            "-Dmaven.javadoc.skip=true",
            "-Danimal.sniffer.skip=true",
            "-Denforcer.skip=true",
            "-DfailIfNoTests=false" };

      final String[] vars = new String[commandLineAddition.length + originals.length];
      for (int i = 0; i < originals.length; i++) {
         vars[i] = originals[i];
      }
      for (int i = 0; i < commandLineAddition.length; i++) {
         vars[originals.length + i] = commandLineAddition[i];
      }

      return buildFolderProcess(folders.getProjectFolder(), logFile, vars);
   }

   private void clean(final File logFile) throws IOException, InterruptedException {
      final String[] originalsClean = new String[] { "mvn", "clean" };
      final ProcessBuilder pbClean = new ProcessBuilder(originalsClean);
      pbClean.directory(folders.getProjectFolder());
      if (logFile != null) {
         pbClean.redirectOutput(Redirect.appendTo(logFile));
         pbClean.redirectError(Redirect.appendTo(logFile));
      }

      final Process processClean = pbClean.start();
      processClean.waitFor();
   }

   @Override
   public void prepareKoPeMeExecution(final File logFile) throws IOException, InterruptedException {
      MavenPomUtil.cleanSnapshotDependencies(new File(folders.getProjectFolder(), "pom.xml"));
      clean(logFile);
      LOG.debug("Starting Test Transformation");
      transformTests();
      if (testTransformer.isUseKieker()) {
         generateAOPXML();
      }
      preparePom();
   }

   @Override
   public void executeTest(final TestCase test, final File logFolder, final long timeout) {
      final File logFile = new File(logFolder, "log_" + test.getClazz() + File.separator + test.getMethod() + ".txt");
      if (!logFile.getParentFile().exists()) {
         logFile.getParentFile().mkdir();
      }
      runTest(logFile, test.getClazz() + "#" + test.getMethod(), timeout);
   }

   /**
    * Runs the given test and saves the results to the result folder.
    * 
    * @param specialResultFolder Folder for saving the results
    * @param testname Name of the test that should be run
    */
   private void runTest(final File logFile, final String testname, final long timeout) {
      try {
         final Process process = buildProcess(logFile, "-Dtest=" + testname);
         execute(testname, timeout, process);
      } catch (final InterruptedException | IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   @Override
   public boolean isVersionRunning(final String version) {
      final File potentialPom = new File(folders.getProjectFolder(), "pom.xml");
      final File testFolder = new File(folders.getProjectFolder(), "src/test");
      final boolean isRunning = false;
      buildfileExists = potentialPom.exists();
      if (potentialPom.exists()) {
         try {
            final boolean multimodule = MavenPomUtil.isMultiModuleProject(potentialPom);
            if (multimodule || testFolder.exists()) {
               MavenPomUtil.cleanSnapshotDependencies(new File(folders.getProjectFolder(), "pom.xml"));
               MavenPomUtil.cleanType(new File(folders.getProjectFolder(), "pom.xml"));
               return testRunningSuccess(version,
                     new String[] { "mvn", "clean", "test-compile",
                           "-DskipTests=true",
                           "-Dmaven.test.skip.exec",
                           "-Dcheckstyle.skip=true",
                           "-Dmaven.compiler.source=" + JAVA_VERSION,
                           "-Dmaven.compiler.target=" + JAVA_VERSION,
                           "-Dmaven.javadoc.skip=true",
                           "-Danimal.sniffer.skip=true",
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
         final File pomFile = new File(folders.getProjectFolder(), "pom.xml");
         final File tempFile = Files.createTempDirectory("kiekerTemp").toFile();
         for (final File module : MavenPomUtil.getModules(pomFile)) {
            editOnePom(true, new File(module, "pom.xml"), tempFile);
         }
         lastTmpFile = tempFile;
      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   private void editOnePom(final boolean update, final File pomFile, final File tempFile) {
      final MavenXpp3Reader reader = new MavenXpp3Reader();
      try {
         final Model model = reader.read(new FileInputStream(pomFile));
         if (model.getBuild() == null) {
            model.setBuild(new Build());
         }
         final String argline;
         if (testTransformer.isUseKieker()) {

            argline = KIEKER_ARG_LINE + " " + TEMP_DIR + "=" + tempFile.toString();
         } else {
            argline = "";
         }

         MavenPomUtil.extendSurefire(argline, model, update, timeout * 2);
         MavenPomUtil.extendDependencies(model, testTransformer.isJUnit3());
         // MavenPomUtil.setCompiler8(model);

         final MavenXpp3Writer writer = new MavenXpp3Writer();
         writer.write(new FileWriter(pomFile), model);

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
