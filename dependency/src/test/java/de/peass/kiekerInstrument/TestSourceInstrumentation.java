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
      
      File testFile = new File(TestConstants.CURRENT_FOLDER, "C0_0.java");
      
      final URL exampleClass = this.getClass().getResource("/sourceInstrumentation/project_2/src/main/java/de/peass/C0_0.java");
      FileUtils.copyURLToFile(exampleClass, testFile);
      
      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.OPERATIONEXECUTION);
      instrumenter.instrument(testFile);
      
      String changedSource = FileUtils.readFileToString(testFile, StandardCharsets.UTF_8);
      
      
      Assert.assertThat(changedSource, Matchers.containsString("MonitoringController.getInstance().isMonitoringEnabled()"));
      Assert.assertThat(changedSource, Matchers.containsString("de.peass.C0_0.method0"));
      
      Assert.assertThat(changedSource, Matchers.containsString("OperationExecutionRecord"));
   }
}
