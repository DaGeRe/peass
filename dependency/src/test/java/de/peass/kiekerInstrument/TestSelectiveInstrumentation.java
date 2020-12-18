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

public class TestSelectiveInstrumentation {
   @Test
   public void testSingleSelectiveInstrumentation() throws Exception {
      SourceInstrumentationTestUtil.initProject("/sourceInstrumentation/project_2/");

      Set<String> shouldInstrument = new HashSet<>();
      shouldInstrument.add("public void de.peass.MainTest.testMe()");
      
      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.OPERATIONEXECUTION, shouldInstrument);
      instrumenter.instrumentProject(TestConstants.CURRENT_FOLDER);

      TestSourceInstrumentation.testFileIsInstrumented(new File(TestConstants.CURRENT_FOLDER, "src/test/java/de/peass/MainTest.java"), 
            "public void de.peass.MainTest.testMe()");
      
      testFileIsNotInstrumented(new File(TestConstants.CURRENT_FOLDER, "src/main/java/de/peass/C0_0.java"), 
            "public void de.peass.C0_0.method0()");
      testFileIsNotInstrumented(new File(TestConstants.CURRENT_FOLDER, "src/main/java/de/peass/C1_0.java"), 
            "public void de.peass.C1_0.method0()");
   }
   
   public static void testFileIsNotInstrumented(File testFile, String fqn) throws IOException {
      String changedSource = FileUtils.readFileToString(testFile, StandardCharsets.UTF_8);

      Assert.assertThat(changedSource, Matchers.not(Matchers.containsString("MonitoringController.getInstance().isMonitoringEnabled()")));
      Assert.assertThat(changedSource, Matchers.not(Matchers.containsString(fqn)));
      Assert.assertThat(changedSource, Matchers.not(Matchers.containsString("new OperationExecutionRecord")));
      Assert.assertThat(changedSource, Matchers.not(Matchers.containsString("kieker.monitoring.core.controller.MonitoringController")));
   }
}
