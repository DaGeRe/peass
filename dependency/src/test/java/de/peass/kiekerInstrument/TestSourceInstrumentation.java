package de.peass.kiekerInstrument;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.peass.TestConstants;
import de.peass.dependency.execution.AllowedKiekerRecord;

public class TestSourceInstrumentation {

   @Test
   public void testSingleClass() throws IOException {
      TestConstants.CURRENT_FOLDER.mkdirs();

      File testFile = SourceInstrumentationTestUtil.copyResource("src/main/java/de/peass/C0_0.java", "/sourceInstrumentation/project_2/");

      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.OPERATIONEXECUTION);
      instrumenter.instrument(testFile);

      testFileIsInstrumented(testFile, "public void de.peass.C0_0.method0()");
   }

   public static void testFileIsInstrumented(File testFile, String fqn) throws IOException {
      String changedSource = FileUtils.readFileToString(testFile, StandardCharsets.UTF_8);

      Assert.assertThat(changedSource, Matchers.containsString("import kieker.monitoring.core.controller.MonitoringController;"));
      Assert.assertThat(changedSource, Matchers.containsString("import kieker.monitoring.core.registry.ControlFlowRegistry;"));
      Assert.assertThat(changedSource, Matchers.containsString("import kieker.monitoring.core.registry.SessionRegistry;"));

      Assert.assertThat(changedSource, Matchers.containsString("MonitoringController.getInstance().isMonitoringEnabled()"));
      Assert.assertThat(changedSource, Matchers.containsString(fqn));
      Assert.assertThat(changedSource, Matchers.containsString("new OperationExecutionRecord"));
   }

   @Test
   public void testProjectInstrumentation() throws IOException {
      SourceInstrumentationTestUtil.initProject("/sourceInstrumentation/project_2/");

      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.OPERATIONEXECUTION);
      instrumenter.instrumentProject(TestConstants.CURRENT_FOLDER);

      testFileIsInstrumented(new File(TestConstants.CURRENT_FOLDER, "src/main/java/de/peass/C0_0.java"), "public void de.peass.C0_0.method0()");
      testFileIsInstrumented(new File(TestConstants.CURRENT_FOLDER, "src/main/java/de/peass/C0_0.java"), "return");
      testFileIsInstrumented(new File(TestConstants.CURRENT_FOLDER, "src/main/java/de/peass/C1_0.java"), "public void de.peass.C1_0.method0()");
      testFileIsInstrumented(new File(TestConstants.CURRENT_FOLDER, "src/main/java/de/peass/AddRandomNumbers.java"), "public int de.peass.AddRandomNumbers.getValue()");
      testFileIsInstrumented(new File(TestConstants.CURRENT_FOLDER, "/src/test/java/de/peass/MainTest.java"), "public void de.peass.MainTest.testMe()");
      testFileIsInstrumented(new File(TestConstants.CURRENT_FOLDER, "/src/test/java/de/peass/MainTest.java"), "public de.peass.MainTest()");
   }
}
