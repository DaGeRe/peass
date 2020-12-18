package de.peass.reading;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.github.javaparser.ParseException;

import de.peass.dependency.analysis.CalledMethodLoader;
import de.peass.dependency.analysis.ModuleClassMapping;
import de.peass.dependency.execution.MavenPomUtil;
import de.peass.dependency.execution.MavenTestExecutor;
import de.peass.dependency.traces.TraceMethodReader;
import de.peass.dependency.traces.TraceWithMethods;
import de.peass.utils.StreamGobbler;

/**
 * Testet, ob bei einem Trace, bei dem eine Methode oder eine Folge von Methoden mehrmals aufgerufen wird, das Lesen des Traces korrekt funktioniert.
 * 
 * Davor muss de.peran.example.Caller mit Kieker-Instrumentierung aufgerufen worden sein.
 * 
 * @author reichelt
 *
 */
public class TestTraceMethodReader {

   private final File tmpFolder = new File("target/kieker_results_test/");
   private final String REPO = System.getenv("HOME") + "/.m2/repository";
   private final String KOPEME_JAR = REPO + "/de/dagere/kopeme/kopeme-core/" + MavenPomUtil.KOPEME_CORE_VERSION + "/kopeme-core-" + MavenPomUtil.KOPEME_CORE_VERSION + ".jar";

   @Before
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
      final ProcessBuilder builder = new ProcessBuilder("java",
            "-javaagent:" + MavenTestExecutor.KIEKER_ASPECTJ_JAR,
            "-Dorg.aspectj.weaver.loadtime.configuration=file:src/test/resources/aop.xml",
            "-cp", KOPEME_JAR + ":target/test-classes/",
            "de.peass.example.CallerSimpleFor");
      final Process process = builder.start();

      StreamGobbler.showFullProcess(process);

      final File[] kiekerFolders = tmpFolder.listFiles((FileFilter) new WildcardFileFilter("kieker-*"));

      final File traceFolder = kiekerFolders[0];
      //
      final TraceMethodReader reader = new TraceMethodReader(new CalledMethodLoader(traceFolder, ModuleClassMapping.SINGLE_MODULE_MAPPING).getShortTrace(""), new File("src/test/java"));
      final TraceWithMethods trace = reader.getTraceWithMethods();

      System.out.println(trace.getWholeTrace());

      /*
       * Trace: 20 Methodenaufrufe -> 10 sonstige -> 10 mal Schleife callMe2 -> müsste auf 1 Eintrag (callMe2, *10 Aufrufe) zusammenfasst werden -> Ziel: 11 Aufrufe
       */
      Assert.assertEquals(11, trace.getLength());
   }

   @Test
   public void testTraceLengthLongFor() throws ParseException, IOException {
      final ProcessBuilder builder = new ProcessBuilder("java",
            "-javaagent:" + MavenTestExecutor.KIEKER_ASPECTJ_JAR,
            "-Dorg.aspectj.weaver.loadtime.configuration=file:src/test/resources/aop.xml",
            "-cp", KOPEME_JAR + ":target/test-classes/",
            "de.peass.example.CallerLongFor");
      final Process process = builder.start();

      StreamGobbler.showFullProcess(process);
      final File[] kiekerFolders = tmpFolder.listFiles((FileFilter) new WildcardFileFilter("kieker-*"));

      final File traceFolder = kiekerFolders[0];

      final TraceMethodReader reader = new TraceMethodReader(new CalledMethodLoader(traceFolder, ModuleClassMapping.SINGLE_MODULE_MAPPING).getShortTrace(""), new File("src/test/java"));
      final TraceWithMethods trace = reader.getTraceWithMethods();

      System.out.println(trace.getWholeTrace());

      Assert.assertEquals(7, trace.getLength());
   }
}
