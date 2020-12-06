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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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

   public static final String SUREFIRE_VERSION = "3.0.0-M3";
   public static final String JAVA_VERSION = "1.8";

   public static final String TEMP_DIR = "-Djava.io.tmpdir";
   public static final String JAVA_AGENT = "-javaagent";
   public static final String KIEKER_VERSION = "1.14";
   public static final String KIEKER_FOLDER_MAVEN = "${user.home}/.m2/repository/net/kieker-monitoring/kieker/" + KIEKER_VERSION +
         "/kieker-" + KIEKER_VERSION + "-aspectj.jar";
   public static final String KIEKER_FOLDER_MAVEN_TWEAK = "${user.home}/.m2/repository/net/kieker-monitoring/kieker/" + KIEKER_VERSION + "/kieker-" + KIEKER_VERSION
         + "-aspectj.jar";
   public static final String KIEKER_FOLDER_GRADLE = "${System.properties['user.home']}/.m2/repository/net/kieker-monitoring/kieker/" + KIEKER_VERSION + "/kieker-" + KIEKER_VERSION
         + "1.14-aspectj.jar";
   public static final String KIEKER_ADAPTIVE_FILENAME = "config/kieker.adaptiveMonitoring.conf";
   public static final File KIEKER_ASPECTJ_JAR = new File(MavenTestExecutor.KIEKER_FOLDER_MAVEN_TWEAK.replace("${user.home}", System.getProperty("user.home")));
   /**
    * This is added to surefire, assuming that kieker has been downloaded already, so that the aspectj-weaving can take place.
    */
   protected static final String KIEKER_ARG_LINE = JAVA_AGENT + ":" + KIEKER_FOLDER_MAVEN;
   protected static final String KIEKER_ARG_LINE_TWEAK = JAVA_AGENT + ":" + KIEKER_FOLDER_MAVEN_TWEAK;

   protected Charset lastEncoding = StandardCharsets.UTF_8;

   private Set<String> includedMethodPattern;

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

   protected void generateAOPXML(AllowedKiekerRecord aspect) {
      try {
         for (final File module : getModules()) {
            for (final String potentialReadFolder : new String[] { "src/main/resources/META-INF", "src/java/META-INF", "src/test/resources/META-INF", "src/test/META-INF",
                  "target/test-classes/META-INF" }) {
               final File folder = new File(module, potentialReadFolder);
               folder.mkdirs();
               final File goalFile2 = new File(folder, "aop.xml");
               AOPXMLHelper.writeAOPXMLToFile(existingClasses, goalFile2, aspect);
               final File propertiesFile = new File(folder, "kieker.monitoring.properties");
               AOPXMLHelper.writeKiekerMonitoringProperties(propertiesFile, aspect.equals(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION));
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
   public void prepareKoPeMeExecution(final File logFile) throws IOException, InterruptedException {
      MavenPomUtil.cleanSnapshotDependencies(new File(folders.getProjectFolder(), "pom.xml"));
      clean(logFile);
      LOG.debug("Starting Test Transformation");
      transformTests();
      if (testTransformer.getConfig().isUseKieker()) {
         if (testTransformer.isAdaptiveExecution()) {
            prepareAdaptiveExecution();
         }
         if (AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION.equals(testTransformer.getConfig().getRecord()) && testTransformer.isAdaptiveExecution()) {
            generateAOPXML(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION);
         } else {
            generateAOPXML(AllowedKiekerRecord.OPERATIONEXECUTION);
         }
         if (testTransformer.isAggregatedWriter()) {

         }
      }
      preparePom();
   }

   public void prepareAdaptiveExecution() throws IOException, InterruptedException {
      if (!MavenTestExecutor.KIEKER_ASPECTJ_JAR.exists()) {
         // This can be removed if Kieker 1.14 is released
         throw new RuntimeException("Tweaked Kieker " + MavenTestExecutor.KIEKER_ASPECTJ_JAR + " needs to exist - git clone https://github.com/DaGeRe/kieker -b 1_13_tweak "
               + "and install manually!");
      }
      writeConfig();
   }

   private void writeConfig() throws IOException {
      final File configFolder = new File(folders.getProjectFolder(), "config");
      configFolder.mkdir();

      final File adaptiveFile = new File(folders.getProjectFolder(), MavenTestExecutor.KIEKER_ADAPTIVE_FILENAME);
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(adaptiveFile))) {
         writer.write("- *\n");
         for (final String includedMethod : includedMethodPattern) {
            writer.write("+ " + includedMethod + "\n");
         }

         writer.flush();
      }
   }

   @Override
   public void executeTest(final TestCase test, final File logFolder, final long timeout) {
      final File module = new File(folders.getProjectFolder(), test.getModule());
      final ChangedEntity testClazzEntity = new ChangedEntity(test.getClazz(), test.getModule());
      runMethod(logFolder, testClazzEntity, module, test.getMethod(), timeout);
   }

   /**
    * Runs the given test and saves the results to the result folder.
    * 
    * @param specialResultFolder Folder for saving the results
    * @param testname Name of the test that should be run
    */
   protected void runTest(final File module, final File logFile, final String testname, final long timeout) {
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
         final File pomFile = new File(folders.getProjectFolder(), "pom.xml");
         final File tempFile = Files.createTempDirectory(folders.getKiekerTempFolder().toPath(), "kiekerTemp").toFile();
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
         final String argline = buildArgline(tempFile);

         MavenPomUtil.extendSurefire(argline, model, update, testTransformer.getConfig().getTimeoutInMinutes() * 2);

         // TODO Move back to extend dependencies, if stable Kieker version supports <init>
         if (model.getDependencies() == null) {
            model.setDependencies(new LinkedList<Dependency>());
         }
         if (testTransformer.isAdaptiveExecution()) {
            // Needs to be the first dependency..
            final List<Dependency> dependencies = model.getDependencies();
            final Dependency kopeme_dependency2 = MavenPomUtil.getDependency("net.kieker-monitoring", KIEKER_VERSION, "test", "kieker");
            dependencies.add(kopeme_dependency2);
         }
         MavenPomUtil.extendDependencies(model, testTransformer.isJUnit3());

         final MavenXpp3Writer writer = new MavenXpp3Writer();
         writer.write(new FileWriter(pomFile), model);

         lastEncoding = MavenPomUtil.getEncoding(model);
      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   private String buildArgline(final File tempFile) {
      final String argline;
      if (testTransformer.getConfig().isUseKieker()) {
         String writerConfig;
         if (testTransformer.isAggregatedWriter()) {
            final String bulkFolder = "-Dkieker.monitoring.writer.filesystem.AggregatedTreeWriter.customStoragePath=" + tempFile.getAbsolutePath().toString();
            writerConfig = "-Dkieker.monitoring.writer=kieker.monitoring.writer.filesystem.AggregatedTreeWriter" +
                  " -Dkieker.monitoring.writer.filesystem.AggregatedTreeWriter.writeInterval=" + testTransformer.getConfig().getKiekerAggregationInterval() +
                  " " + bulkFolder;

            if (testTransformer.isIgnoreEOIs()) {
               writerConfig += " -Dkieker.monitoring.writer.filesystem.AggregatedTreeWriter.ignoreEOIs=true";
            }

            // if (testTransformer.isSplitAggregated()) {
            // writerConfig += " -Dkieker.monitoring.writer.filesystem.AggregatedTreeWriter.aggregateSplitted=true";
            // }

         } else {
            writerConfig = "";
         }

         if (!testTransformer.isAdaptiveExecution()) {
            argline = KIEKER_ARG_LINE +
                  " " + TEMP_DIR + "=" + tempFile.getAbsolutePath().toString() +
                  " " + writerConfig;
         } else {
            argline = KIEKER_ARG_LINE_TWEAK +
                  " " + TEMP_DIR + "=" + tempFile.getAbsolutePath().toString() +
                  " -Dkieker.monitoring.adaptiveMonitoring.enabled=true" +
                  " -Dkieker.monitoring.adaptiveMonitoring.configFile=" + KIEKER_ADAPTIVE_FILENAME +
                  " -Dkieker.monitoring.adaptiveMonitoring.readInterval=15" +
                  " " + writerConfig;
         }
      } else {
         argline = "";
      }
      return argline;
   }

   public Charset getEncoding() {
      return lastEncoding;
   }

   @Override
   public List<File> getModules() throws IOException, XmlPullParserException {
      return MavenPomUtil.getModules(new File(folders.getProjectFolder(), "pom.xml"));
   }

   @Override
   public void setIncludedMethods(final Set<String> includedMethodPattern) {
      this.includedMethodPattern = includedMethodPattern;
   }

}
