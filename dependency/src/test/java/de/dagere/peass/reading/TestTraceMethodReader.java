package de.dagere.peass.reading;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.javaparser.ParseException;

import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.analysis.CalledMethodLoader;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.traces.TraceMethodReader;
import de.dagere.peass.dependency.traces.TraceWithMethods;
import de.dagere.peass.execution.maven.pom.MavenPomUtil;
import de.dagere.peass.execution.maven.pom.MavenTestExecutor;
import de.dagere.peass.utils.StreamGobbler;

/**
 * Testet, ob bei einem Trace, bei dem eine Methode oder eine Folge von Methoden mehrmals aufgerufen wird, das Lesen des Traces korrekt funktioniert.
 * 
 * Davor muss de.peran.example.Caller mit Kieker-Instrumentierung aufgerufen worden sein.
 * 
 * @author reichelt
 *
 */
public class TestTraceMethodReader {

   // Usually, the both following versions need to be updated together
   private static final String LOG4J_VERSION = "2.20.0";
   private static final String SLF4J_IMPL_VERSION = "2.20.0";

   private final File tmpFolder = new File("target" + File.separator + "kieker_results_test");
   private static final String REPO = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
   private static final String KOPEME_JAR = REPO + File.separator + "de" + File.separator + "dagere" + File.separator + "kopeme" + File.separator + "kopeme-core" + File.separator
         + MavenPomUtil.KOPEME_VERSION + File.separator + "kopeme-core-" + MavenPomUtil.KOPEME_VERSION + ".jar";
   private static final String SLF4J_IMPL_JAR = REPO + File.separator + "org" + File.separator + "apache" + File.separator + "logging" + File.separator + "log4j" + File.separator
         + "log4j-slf4j-impl" + File.separator + SLF4J_IMPL_VERSION + File.separator + "log4j-slf4j-impl-" + SLF4J_IMPL_VERSION + ".jar";
   private static final String SLF4J_API_VERSION = "1.7.30";
   private static final String SLF4J_API_JAR = REPO + File.separator + "org" + File.separator + "slf4j" + File.separator + "slf4j-api" + File.separator + SLF4J_API_VERSION
         + File.separator + "slf4j-api-" + SLF4J_API_VERSION + ".jar";
   private static final String LOG4J_FOLDER = REPO + File.separator + "org" + File.separator + "apache" + File.separator + "logging" + File.separator + "log4j";
   private static final String LOG4J_IMPL_JAR = LOG4J_FOLDER + File.separator + "log4j-core" + File.separator + LOG4J_VERSION + File.separator + File.separator
         + "log4j-core-" + LOG4J_VERSION + ".jar";
   private static final String LOG4J_API_JAR = LOG4J_FOLDER + File.separator + "log4j-api" + File.separator + LOG4J_VERSION + File.separator + "log4j-api-" + LOG4J_VERSION
         + ".jar";

   @BeforeEach
   public void init() {
      tmpFolder.mkdirs();
      final File[] kiekerFoldersOld = tmpFolder.listFiles((FileFilter) new WildcardFileFilter("kieker*"));
      if (kiekerFoldersOld != null) {
         for (final File kiekerFolderOld : kiekerFoldersOld) {
            try {
               FileUtils.deleteDirectory(kiekerFolderOld);
            } catch (final IOException e) {
               e.printStackTrace();
            }
         }
      }
   }

   @Test
   public void testTraceLengthSimpleFor() throws ParseException, IOException {
      System.out.println("Searching: " + SLF4J_API_JAR + " " + new File(SLF4J_API_JAR).exists());
      System.out.println("Searching: " + SLF4J_IMPL_JAR + " " + new File(SLF4J_IMPL_JAR).exists());

      String jarPath = getJarPath();
      String agentPath = getAgentPath();

      final ProcessBuilder builder = new ProcessBuilder("java",
            "-javaagent:" + agentPath,
            "-Dorg.aspectj.weaver.loadtime.configuration=file:src" + File.separator + "test" + File.separator + "resources" + File.separator + "aop.xml",
            "-cp", jarPath,
            "de.dagere.peass.example.CallerSimpleFor");
      System.out.println("Command: " + builder.command());
      final Process process = builder.start();

      StreamGobbler.showFullProcess(process);

      final File[] kiekerFolders = tmpFolder.listFiles((FileFilter) new WildcardFileFilter("kieker-*"));

      final File traceFolder = kiekerFolders[0];
      //
      final TraceMethodReader reader = new TraceMethodReader(new CalledMethodLoader(traceFolder, ModuleClassMapping.SINGLE_MODULE_MAPPING, new KiekerConfig()).getShortTrace(""),
            new File("src" + File.separator + "test" + File.separator + "java"));
      final TraceWithMethods trace = reader.getTraceWithMethods();

      System.out.println(trace.getWholeTrace());

      /*
       * Trace: 20 Methodenaufrufe -> 10 sonstige -> 10 mal Schleife callMe2 -> müsste auf 1 Eintrag (callMe2, *10 Aufrufe) zusammenfasst werden -> Ziel: 11 Aufrufe
       */
      Assert.assertEquals(11, trace.getLength());
   }

   private String getAgentPath() {
      String agentPath;
      if (!System.getProperty("os.name").startsWith("Windows")) {
         agentPath = MavenTestExecutor.KIEKER_ASPECTJ_JAR.getAbsolutePath();
      } else {
         agentPath = "\"" + MavenTestExecutor.KIEKER_ASPECTJ_JAR.getAbsolutePath() + "\"";
      }
      System.out.println(agentPath);
      return agentPath;
   }

   private String getJarPath() {
      String jarPath;
      if (!System.getProperty("os.name").startsWith("Windows")) {
         jarPath = KOPEME_JAR + File.pathSeparator + SLF4J_API_JAR + File.pathSeparator + SLF4J_IMPL_JAR + File.pathSeparator +
               LOG4J_IMPL_JAR + File.pathSeparator + LOG4J_API_JAR + File.pathSeparator + "target" + File.separator + "test-classes";
      } else {
         jarPath = "\"" + KOPEME_JAR + "\"" + File.pathSeparator +
               "\"" + SLF4J_API_JAR + "\"" + File.pathSeparator +
               "\"" + SLF4J_IMPL_JAR + "\"" + File.pathSeparator +
               "\"" + LOG4J_IMPL_JAR + "\"" + File.pathSeparator +
               "\"" + LOG4J_API_JAR + "\"" + File.pathSeparator +
               "target" + File.separator + "test-classes";
      }
      System.out.println(jarPath);
      return jarPath;
   }

   @Test
   public void testTraceLengthLongFor() throws ParseException, IOException {
      String jarPath = getJarPath();
      String agentPath = getAgentPath();

      final ProcessBuilder builder = new ProcessBuilder("java",
            "-javaagent:" + agentPath,
            "-Dorg.aspectj.weaver.loadtime.configuration=file:src" + File.separator + "test" + File.separator + "resources" + File.separator + "aop.xml",
            "-cp", jarPath,
            "de.dagere.peass.example.CallerLongFor");
      final Process process = builder.start();

      StreamGobbler.showFullProcess(process);
      final File[] kiekerFolders = tmpFolder.listFiles((FileFilter) new WildcardFileFilter("kieker-*"));

      final File traceFolder = kiekerFolders[0];

      final TraceMethodReader reader = new TraceMethodReader(new CalledMethodLoader(traceFolder, ModuleClassMapping.SINGLE_MODULE_MAPPING, new KiekerConfig()).getShortTrace(""),
            new File("src" + File.separator + "test" + File.separator + "java"));
      final TraceWithMethods trace = reader.getTraceWithMethods();

      System.out.println(trace.getWholeTrace());

      Assert.assertEquals(7, trace.getLength());
   }
}
