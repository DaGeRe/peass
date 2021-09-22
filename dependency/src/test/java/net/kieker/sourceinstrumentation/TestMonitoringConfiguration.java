package net.kieker.sourceinstrumentation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import de.dagere.peass.TestConstants;
import net.kieker.sourceinstrumentation.instrument.InstrumentKiekerSource;

public class TestMonitoringConfiguration {

   @Test
   public void testAdaptiveMonitoringDeactivated() throws IOException {
      TestConstants.CURRENT_FOLDER.mkdirs();

      File testFile = SourceInstrumentationTestUtil.copyResource("src/main/java/de/peass/C0_0.java", "/sourceInstrumentation/project_2/");

      HashSet<String> includedPatterns = new HashSet<>();
      includedPatterns.add("*");
      InstrumentationConfiguration kiekerConfiguration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, includedPatterns, false, true, 1000, false);
      
      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(kiekerConfiguration);
      instrumenter.instrument(testFile);

      TestSourceInstrumentation.testFileIsInstrumented(testFile, "public void de.peass.C0_0.method0()", "OperationExecutionRecord");
      String changedSource = FileUtils.readFileToString(testFile, StandardCharsets.UTF_8);
      MatcherAssert.assertThat(changedSource, Matchers.not(Matchers.containsString("isProbeActivated")));
   }

   @Test
   public void testDeactivationDeactivated() throws IOException {
      TestConstants.CURRENT_FOLDER.mkdirs();

      File testFile = SourceInstrumentationTestUtil.copyResource("src/main/java/de/peass/C0_0.java", "/sourceInstrumentation/project_2/");

      HashSet<String> includedPatterns = new HashSet<>();
      includedPatterns.add("*");
      InstrumentationConfiguration kiekerConfiguration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, includedPatterns, true, false, 1000, false);
      
      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(kiekerConfiguration);
      instrumenter.instrument(testFile);

      TestSourceInstrumentation.testFileIsInstrumented(testFile, "public void de.peass.C0_0.method0()", "OperationExecutionRecord");
      String changedSource = FileUtils.readFileToString(testFile, StandardCharsets.UTF_8);
      MatcherAssert.assertThat(changedSource, Matchers.not(Matchers.containsString("isMonitoringEnabled")));
   }
   
   @Test
   public void testBothDeactivated() throws IOException {
      TestConstants.CURRENT_FOLDER.mkdirs();

      File testFile = SourceInstrumentationTestUtil.copyResource("src/main/java/de/peass/C0_0.java", "/sourceInstrumentation/project_2/");

      HashSet<String> includedPatterns = new HashSet<>();
      includedPatterns.add("*");
      InstrumentationConfiguration kiekerConfiguration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, includedPatterns, false, false, 1000, false);
      
      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(kiekerConfiguration);
      instrumenter.instrument(testFile);

      TestSourceInstrumentation.testFileIsInstrumented(testFile, "public void de.peass.C0_0.method0()", "OperationExecutionRecord");
      String changedSource = FileUtils.readFileToString(testFile, StandardCharsets.UTF_8);
      MatcherAssert.assertThat(changedSource, Matchers.not(Matchers.containsString("isProbeActivated")));
      MatcherAssert.assertThat(changedSource, Matchers.not(Matchers.containsString("isMonitoringEnabled")));
      MatcherAssert.assertThat(changedSource, Matchers.not(Matchers.containsString("return")));
   }
   
   @Test
   public void testBothDeactivatedConstructor() throws IOException {
      TestConstants.CURRENT_FOLDER.mkdirs();

      File testFile = SourceInstrumentationTestUtil.copyResource("src/test/java/de/peass/MainTest.java", "/sourceInstrumentation/project_2/");

      HashSet<String> includedPatterns = new HashSet<>();
      includedPatterns.add("*");
      InstrumentationConfiguration kiekerConfiguration = new InstrumentationConfiguration(AllowedKiekerRecord.OPERATIONEXECUTION, false, includedPatterns, false, false, 1000, false);
      
      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(kiekerConfiguration);
      instrumenter.instrument(testFile);

      TestSourceInstrumentation.testFileIsInstrumented(testFile, "public new de.peass.MainTest.<init>()", "OperationExecutionRecord");
      String changedSource = FileUtils.readFileToString(testFile, StandardCharsets.UTF_8);
      MatcherAssert.assertThat(changedSource, Matchers.not(Matchers.containsString("isProbeActivated")));
      MatcherAssert.assertThat(changedSource, Matchers.not(Matchers.containsString("isMonitoringEnabled")));
      
      System.out.println(changedSource);
      
      MatcherAssert.assertThat(changedSource, Matchers.not(Matchers.containsString("return")));
   }
}
