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
package de.peran.dependency.execution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import de.peran.dependency.ClazzFinder;
import de.peran.dependency.analysis.data.ChangedEntity;
import de.peran.dependency.analysis.data.TestSet;
import de.peran.testtransformation.JUnitTestTransformer;

/**
 * Organizes the running of tests in a maven project by enhancing the pom, changing the test classes and calling the maven test goal
 * 
 * @author reichelt
 *
 */
public class MavenKiekerTestExecutor extends TestExecutor {

   private static final Logger LOG = LogManager.getLogger(MavenKiekerTestExecutor.class);

   public static final String ORG_APACHE_MAVEN_PLUGINS = "org.apache.maven.plugins";
   public static final String SUREFIRE_ARTIFACTID = "maven-surefire-plugin";
   public static final String SUREFIRE_VERSION = "2.21.0";
   public static final String JAVA_VERSION = "1.8";

   /**
    * This is added to surefire, assuming that kieker has been downloaded already, so that the aspectj-weaving can take place.
    */
   protected static final String KIEKER_ARG_LINE = "-javaagent:" + System.getProperty("user.home") + "/.m2/repository/net/kieker-monitoring/kieker/1.13/kieker-1.13-aspectj.jar";

   protected Charset lastEncoding = StandardCharsets.UTF_8;
   protected final JUnitTestTransformer testGenerator;

   public MavenKiekerTestExecutor(final File projectFolder, final File resultsFolder, JUnitTestTransformer testtransformer) {
      super(projectFolder, resultsFolder);
      this.testGenerator = testtransformer;
   }

   protected void generateAOPXML() throws IOException {
      try {
         final File pom = new File(projectFolder, "pom.xml");
         final List<File> modules = MavenPomUtil.getModules(pom);
         final List<String> allClasses = new LinkedList<>();
         for (final File module : modules) {
            final List<String> classes = ClazzFinder.getClasses(module);
            allClasses.addAll(classes);
         }
         for (final File module : modules) {
            final File metainf = new File(module, "src/main/resources/META-INF");
            metainf.mkdirs();
            final File goalFile = new File(metainf, "aop.xml");
            writeAOPXMLToFile(allClasses, goalFile);

            final File metainfTarget = new File(module, "target/test-classes/META-INF");
            metainfTarget.mkdirs();
            final File goalFileTarget = new File(metainfTarget, "aop.xml");
            writeAOPXMLToFile(allClasses, goalFileTarget);
            final File goalFileProperties = new File(metainfTarget, "kieker.monitoring.properties");
            final InputStream resourceAsStream = MavenKiekerTestExecutor.class.getResourceAsStream("/copy/kieker.monitoring.properties");
            try (FileWriter output = new FileWriter(goalFileProperties)){
               IOUtils.copy(resourceAsStream, output, StandardCharsets.UTF_8);
               output.flush();
            }
           
            final File metainf2 = new File(module, "src/java/META-INF");
            metainf2.mkdirs();
            final File goalFile2 = new File(metainf2, "aop.xml");
            writeAOPXMLToFile(allClasses, goalFile2);
         }
      } catch (final XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   private void writeAOPXMLToFile(final List<String> allClasses, File goalFile) throws IOException {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(goalFile))) {
         writer.write("<!DOCTYPE aspectj PUBLIC \"-//AspectJ//DTD//EN\" \"http://www.aspectj.org/dtd/aspectj_1_5_0.dtd\">\n");
         writer.write("<aspectj>\n");
         writer.write("	<weaver options=\"-verbose\">\n");
         for (final String clazz : allClasses) {
            if (!clazz.contains("$")) { // Fix: Kieker 1.12 is not able to read inner-class-entries
               writer.write("   <include within=\"" + clazz + "\" />\n");
            }
         }
         writer.write("	</weaver>\n");
         writer.write("	<aspects>");
         writer.write("		<aspect ");
         writer.write("name=\"kieker.monitoring.probe.aspectj.operationExecution.OperationExecutionAspectFull\" />");
         writer.write("	</aspects>\n");
         writer.write("</aspectj>");
         writer.flush();
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
            "-Denforcer.skip=true",
            "-DfailIfNoTests=false" };

      final String[] vars = new String[commandLineAddition.length + originals.length];
      for (int i = 0; i < originals.length; i++) {
         vars[i] = originals[i];
      }
      for (int i = 0; i < commandLineAddition.length; i++) {
         vars[originals.length + i] = commandLineAddition[i];
      }

      LOG.debug("Command: {}", vars);

      final ProcessBuilder pb = new ProcessBuilder(vars);
      LOG.debug("KOPEME_HOME={}", resultsFolder.getAbsolutePath());
      pb.environment().put("KOPEME_HOME", resultsFolder.getAbsolutePath());

      pb.directory(projectFolder);
      if (logFile != null) {
         pb.redirectOutput(Redirect.appendTo(logFile));
         pb.redirectError(Redirect.appendTo(logFile));
      }

      final Process process = pb.start();
      return process;
   }

   private void clean(final File logFile) throws IOException, InterruptedException {
      final String[] originalsClean = new String[] { "mvn", "clean" };
      final ProcessBuilder pbClean = new ProcessBuilder(originalsClean);
      pbClean.directory(projectFolder);
      if (logFile != null) {
         pbClean.redirectOutput(Redirect.appendTo(logFile));
         pbClean.redirectError(Redirect.appendTo(logFile));
      }

      final Process processClean = pbClean.start();
      processClean.waitFor();
   }

   /**
    * Runs all tests and saves the results to the given result folder
    * 
    * @param specialResultFolder Folder for saving the results
    * @param tests Name of the test that should be run
    */
   @Override
   public void executeAllTests(final File logFile) {
      try {
         clean(logFile);
         final boolean compiled = prepareRunning(logFile);
         if (compiled) {
            final Process process = buildProcess(logFile);
            LOG.info("Starting Process");
            process.waitFor();
         }
      } catch (final XmlPullParserException | IOException | InterruptedException e) {
         e.printStackTrace();
      }
   }

   public boolean prepareRunning(final File logFile) {
      preparePom();
      LOG.debug("Starting Test Transformation");
      testGenerator.transformTests();

      LOG.debug("Starting Compilation");
      try {
         generateAOPXML();
         return true;
      } catch (final IOException e) {
         e.printStackTrace();
         return false;
      }
   }

   /**
    * Runs the given tests and saves the results to the given result folder
    * 
    * @param specialResultFolder Folder for saving the results
    * @param tests Name of the test that should be run
    */
   @Override
   public void executeTests(final TestSet tests, final File logFolder) {
      final File globalLogFile = new File(logFolder, "log_compilation.txt");
      try {
         clean(globalLogFile);
      } catch (IOException | InterruptedException e) {
         e.printStackTrace();
      }
      final boolean compiled = prepareRunning(globalLogFile);
      if (compiled) {
         for (final Map.Entry<ChangedEntity, List<String>> clazzEntry : tests.entrySet()) {
            final File logFile = new File(logFolder, "log_" + clazzEntry.getKey().getJavaClazzName() + ".txt");
            if (clazzEntry.getValue().size() > 0) {
               for (final String method : clazzEntry.getValue()) {
                  if (method.length() > 0) {
                     runTest(logFile, clazzEntry.getKey().getJavaClazzName() + "#" + method);
                  } else {
                     runTest(logFile, clazzEntry.getKey().getJavaClazzName());
                  }
               }
            } else {
               runTest(logFile, clazzEntry.getKey().getJavaClazzName());
            }
         }
      }
   }

   /**
    * Runs the given test and saves the results to the result folder.
    * 
    * @param specialResultFolder Folder for saving the results
    * @param testname Name of the test that should be run
    */
   private void runTest(final File logFile, final String testname) {
      try {
         LOG.debug("Executing: {}", testname);
         final Process process = buildProcess(logFile, "-Dtest=" + testname);
         process.waitFor();
      } catch (final InterruptedException | IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   protected boolean testRunning() {
      return MavenPomUtil.testRunning(projectFolder);
   }

   @Override
   public boolean isVersionRunning() {
      final File potentialPom = new File(projectFolder, "pom.xml");
      final File testFolder = new File(projectFolder, "src/test");
      LOG.debug(potentialPom);
      boolean isRunning = false;
      if (potentialPom.exists()) {
         try {
            final boolean multimodule = MavenPomUtil.isMultiModuleProject(potentialPom);
            if (multimodule || testFolder.exists()) {
               isRunning = testVersion(potentialPom);
               LOG.debug("pom.xml existing");
               isRunning = testRunning();
               if (isRunning) {
                  jdk_version = 8;
               } else {
                  final String boot_class_path = System.getenv("BOOT_LIBS");
                  if (boot_class_path != null) {

                     final MavenXpp3Reader reader = new MavenXpp3Reader();
                     final Model model = reader.read(new FileInputStream(potentialPom));
                     if (model.getBuild() == null) {
                        model.setBuild(new Build());
                     }
                     final Plugin compiler = MavenPomUtil.findPlugin(model, MavenKiekerTestExecutor.COMPILER_ARTIFACTID,
                           MavenKiekerTestExecutor.ORG_APACHE_MAVEN_PLUGINS);

                     MavenPomUtil.extendCompiler(compiler, boot_class_path);
                     final MavenXpp3Writer writer = new MavenXpp3Writer();
                     writer.write(new FileWriter(potentialPom), model);

                     isRunning = testRunning();
                     if (isRunning) {
                        jdk_version = 6;
                     }

                  }
               }
            } else {
               LOG.error("Expected src/test to exist");
            }
         } catch (IOException | XmlPullParserException e) {
            e.printStackTrace();
         }
      } else {
         LOG.error("Expected pom.xml to exist");
      }

      return isRunning;
   }

   public void preparePom() {
      preparePom(true);
   }

   public void preparePom(boolean update) {
      try {
         final File pomFile = new File(projectFolder, "pom.xml");
         for (final File module : MavenPomUtil.getModules(pomFile)) {
            editOnePom(update, new File(module, "pom.xml"));
         }
      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   private void editOnePom(boolean update, final File pomFile) {
      final MavenXpp3Reader reader = new MavenXpp3Reader();
      try {
         final Model model = reader.read(new FileInputStream(pomFile));
         if (model.getBuild() == null) {
            model.setBuild(new Build());
         }
         final Plugin surefire = MavenPomUtil.findPlugin(model, SUREFIRE_ARTIFACTID, ORG_APACHE_MAVEN_PLUGINS);

         final Path tempFiles = Files.createTempDirectory("kiekerTemp");
         lastTmpFile = tempFiles.toFile();
         final String argline;
         if (testGenerator.isUseKieker()) {
            argline = KIEKER_ARG_LINE + " -Djava.io.tmpdir=" + tempFiles.toString();
         } else {
            argline = "";
         }

         MavenPomUtil.extendSurefire(argline, surefire, update);
         MavenPomUtil.extendDependencies(model);

         setJDK(model);

         final MavenXpp3Writer writer = new MavenXpp3Writer();
         writer.write(new FileWriter(pomFile), model);

         lastEncoding = MavenPomUtil.getEncoding(model);
      } catch (IOException | XmlPullParserException e) {
         e.printStackTrace();
      }
   }

   protected boolean testVersion(final File potentialPom) {
      try {
         final MavenXpp3Reader reader2 = new MavenXpp3Reader();
         final Model model2 = reader2.read(new FileInputStream(potentialPom));
         final Properties properties = model2.getProperties();
         if (properties != null) {
            final String source = properties.getProperty("maven.compiler.source");
            final String target = properties.getProperty("maven.compiler.target");
            if (target != null && (target.equals("1.3") || target.equals("1.4"))) {
               return false;
            }
            if (source != null && (source.equals("1.3") || source.equals("1.4"))) {
               return false;
            }
         }
         final Plugin compilerPlugin = MavenPomUtil.findPlugin(model2, "maven-compiler-plugin", "org.apache.maven.plugins");
         if (compilerPlugin != null) {
            final Xpp3Dom config = (Xpp3Dom) compilerPlugin.getConfiguration();
            if (config != null) {
               final Xpp3Dom sourceChild = config.getChild("source");
               final Xpp3Dom targetChild = config.getChild("target");
               if (sourceChild != null && targetChild != null) {
                  final String source = sourceChild.getValue();
                  final String target = targetChild.getValue();
                  if (target != null && (target.equals("1.3") || target.equals("1.4") || target.equals("1.5"))) {
                     return false;
                  }
                  if (source != null && (source.equals("1.3") || source.equals("1.4") || target.equals("1.5"))) {
                     return false;
                  }
               }
            }
         }
      } catch (XmlPullParserException | IOException e) {
         e.printStackTrace();
      }
      return true;
   }

   public Charset getEncoding() {
      return lastEncoding;
   }

}
