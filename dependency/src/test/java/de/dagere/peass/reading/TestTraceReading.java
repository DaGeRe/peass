package de.dagere.peass.reading;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.github.javaparser.ParseException;

import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.traces.TraceMethodReader;
import de.dagere.peass.dependency.traces.TraceWithMethods;

/**
 * Tests whether trace reading for the purpose of experiment result analysis is working corretly.
 * 
 * In order to execute this test, first call de.peran.example.Caller with kieker-aspectj-instrumentation (normaly something like
 * -javaagent:~/.m2/repository/net/kieker-monitoring/kieker/1.12/kieker-1.12-aspectj.jar ) and afterwards copy the results to src/test/resources/kieker-example. In the repo, an
 * recent version of the traces should be contained.
 * 
 * @author reichelt
 *
 */
public class TestTraceReading {

   private static final Logger LOG = LogManager.getLogger(TestTraceReading.class);

   private static File TEST_FOLDER = new File("src" + File.separator + "test");
   private static final File EXAMPLE_TRACE_FOLDER = new File(TEST_FOLDER, "resources" + File.separator + "kieker-example");
   private static final File EXAMPLE_SOURCE_FOLDER = new File(TEST_FOLDER, "java");

   @Test
   public void testSimpleTraceReading() throws ParseException, IOException {
      final TraceMethodReader tmr = new TraceMethodReader(new File(EXAMPLE_TRACE_FOLDER, "simple"),
            ModuleClassMapping.SINGLE_MODULE_MAPPING,
            EXAMPLE_SOURCE_FOLDER);
      // The path to the kieker-trace-data (first argument) and the path to the classes must match
      final TraceWithMethods trace = tmr.getTraceWithMethods();
      LOG.info("Trace length: " + trace.getLength());
      for (int i = 0; i < trace.getLength(); i++) {
         LOG.info(trace.getTraceElement(i));
         LOG.info(trace.getMethod(i));
      }

      Assert.assertNull(trace.getMethod(1));

      Assert.assertEquals(trace.getMethod(0).substring(0, trace.getMethod(0).indexOf("\n")), "public static void main(final String[] args) {");
      Assert.assertEquals(trace.getMethod(5).substring(0, trace.getMethod(5).indexOf("\n")), "private void main(final int z) {");
      Assert.assertEquals(trace.getMethod(6).substring(0, trace.getMethod(6).indexOf("\n")), "private void main(final String z) {");
   }

   @Test
   public void testLongTraceReading() throws ParseException, IOException {
      final TraceMethodReader tmr = new TraceMethodReader(new File(EXAMPLE_TRACE_FOLDER, "long"), ModuleClassMapping.SINGLE_MODULE_MAPPING,
            EXAMPLE_SOURCE_FOLDER);
      // The path to the kieker-trace-data (first argument) and the path to the classes must match
      final TraceWithMethods trace = tmr.getTraceWithMethods();
      LOG.info("Trace length: " + trace.getLength());
      for (int i = 0; i < trace.getLength(); i++) {
         LOG.info(trace.getTraceElement(i));
         LOG.info(trace.getMethod(i));
      }

      Assert.assertNull(trace.getMethod(1));

      Assert.assertEquals("public static void main(final String[] args) {", trace.getMethod(0).substring(0, trace.getMethod(0).indexOf("\n")));
      Assert.assertEquals("public void callMe() {", trace.getMethod(2).substring(0, trace.getMethod(2).indexOf("\n")));
      final String sourceMethod5 = trace.getMethod(6);
      Assert.assertEquals("private int callMe4() {", sourceMethod5.substring(0, sourceMethod5.indexOf("\n")));
   }
}
