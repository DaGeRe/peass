package de.peass.kiekerInstrument;

import java.io.File;
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

      final File instrumentedFile = new File(TestConstants.CURRENT_FOLDER, "src/test/java/de/peass/MainTest.java");
      TestSourceInstrumentation.testFileIsInstrumented(instrumentedFile, 
            "public void de.peass.MainTest.testMe()", "ReducedOperationExecutionRecord");
      
      String changedSource = FileUtils.readFileToString(instrumentedFile, StandardCharsets.UTF_8);
      Assert.assertThat(changedSource, Matchers.containsString("if (testMeCounter"));
      Assert.assertThat(changedSource, Matchers.containsString("private static int testMeCounter"));
   }
}
