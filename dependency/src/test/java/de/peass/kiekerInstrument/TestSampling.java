package de.peass.kiekerInstrument;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import de.peass.TestConstants;
import de.peass.dependency.execution.AllowedKiekerRecord;

public class TestSampling {
   @Test
   public void testSingleSelectiveInstrumentation() throws Exception {
      SourceInstrumentationTestUtil.initProject("/sourceInstrumentation/project_2/");

      Set<String> shouldInstrument = new HashSet<>();
      shouldInstrument.add("public void de.peass.MainTest.testMe()");

      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION, shouldInstrument, true);
      instrumenter.instrumentProject(TestConstants.CURRENT_FOLDER);

      testIsSamplingInstrumented("src/test/java/de/peass/MainTest.java", "public void de.peass.MainTest.testMe()", "testMeCounter");
   }

   @Test
   public void testComplexSignatures() throws Exception {
      SourceInstrumentationTestUtil.initProject("/sourceInstrumentation/project_2_signatures/");

      Set<String> shouldInstrument = new HashSet<>();
      shouldInstrument.add("public void de.peass.MainTest.testMe()");
      shouldInstrument.add("public java.lang.String de.peass.C0_0.method0(java.lang.String)");
      shouldInstrument.add("public static void de.peass.C0_0.myStaticStuff()");

      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION, shouldInstrument, true);
      instrumenter.instrumentProject(TestConstants.CURRENT_FOLDER);

      testIsSamplingInstrumented("src/test/java/de/peass/MainTest.java", "public void de.peass.MainTest.testMe()", "testMeCounter");
      testIsSamplingInstrumented("src/main/java/de/peass/C0_0.java", "public java.lang.String de.peass.C0_0.method0(java.lang.String", "method0Counter");
      testIsSamplingInstrumented("src/main/java/de/peass/C0_0.java", "public static void de.peass.C0_0.myStaticStuff()", "myStaticStuffCounter1");
   }

   private void testIsSamplingInstrumented(final String filename, final String instrumentedMethod, final String counterName) throws IOException {
      final File instrumentedFile = new File(TestConstants.CURRENT_FOLDER, filename);
      TestSourceInstrumentation.testFileIsInstrumented(instrumentedFile,
            instrumentedMethod, "ReducedOperationExecutionRecord");

      String changedSource = FileUtils.readFileToString(instrumentedFile, StandardCharsets.UTF_8);
      Assert.assertThat(changedSource, Matchers.containsString("if (" + counterName));
      Assert.assertThat(changedSource, Matchers.containsString("private static int " + counterName));
   }
}
