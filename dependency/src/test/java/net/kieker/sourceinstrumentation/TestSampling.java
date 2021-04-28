package net.kieker.sourceinstrumentation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.TestConstants;
import net.kieker.sourceinstrumentation.instrument.InstrumentKiekerSource;

public class TestSampling {
   @Test
   public void testSingleSelectiveInstrumentation() throws Exception {
      SourceInstrumentationTestUtil.initProject("/sourceInstrumentation/project_2/");

      Set<String> includedPatterns = new HashSet<>();
      includedPatterns.add("public void de.peass.MainTest.testMe()");

      InstrumentationConfiguration kiekerConfiguration = new InstrumentationConfiguration(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION, true, includedPatterns, false, false, 1000);
      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(kiekerConfiguration);
      instrumenter.instrumentProject(TestConstants.CURRENT_FOLDER);

      testIsSamplingInstrumented("src/test/java/de/peass/MainTest.java", "public void de.peass.MainTest.testMe()", "testMeCounter");
   }

   @Test
   public void testComplexSignatures() throws Exception {
      SourceInstrumentationTestUtil.initProject("/sourceInstrumentation/project_2_signatures/");

      Set<String> includedPatterns = new HashSet<>();
      includedPatterns.add("public void de.peass.MainTest.testMe()");
      includedPatterns.add("public * de.peass.MainTest.<init>()");
      includedPatterns.add("public java.lang.String de.peass.C0_0.method0(java.lang.String)");
      includedPatterns.add("public static void de.peass.C0_0.myStaticStuff()");

      InstrumentationConfiguration kiekerConfiguration = new InstrumentationConfiguration(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION, true, includedPatterns, false, false, 1000);
      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(kiekerConfiguration);
      instrumenter.instrumentProject(TestConstants.CURRENT_FOLDER);

      testIsSamplingInstrumented("src/test/java/de/peass/MainTest.java", "public new de.peass.MainTest.<init>()", "initCounter");
      testIsSamplingInstrumented("src/test/java/de/peass/MainTest.java", "public void de.peass.MainTest.testMe()", "testMeCounter");
      testIsSamplingInstrumented("src/main/java/de/peass/C0_0.java", "public java.lang.String de.peass.C0_0.method0(java.lang.String", "method0Counter");
      testIsSamplingInstrumented("src/main/java/de/peass/C0_0.java", "public static void de.peass.C0_0.myStaticStuff()", "myStaticStuffCounter1");
   }

   private void testIsSamplingInstrumented(final String filename, final String instrumentedMethod, final String counterName) throws IOException {
      final File instrumentedFile = new File(TestConstants.CURRENT_FOLDER, filename);
      TestSourceInstrumentation.testFileIsInstrumented(instrumentedFile,
            instrumentedMethod, "ReducedOperationExecutionRecord");

      String changedSource = FileUtils.readFileToString(instrumentedFile, StandardCharsets.UTF_8);
      Assert.assertThat(changedSource, Matchers.containsString("if (" + InstrumentationConstants.PREFIX + counterName));
      Assert.assertThat(changedSource, Matchers.containsString("private static int " + InstrumentationConstants.PREFIX + counterName));
   }
}
