package net.kieker.sourceinstrumentation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.peass.TestConstants;
import net.kieker.sourceinstrumentation.instrument.InstrumentKiekerSource;

public class TestInterfaceInstrumentation {
   @Test
   public void testInterfaceNoPackage() throws IOException {
      TestConstants.CURRENT_FOLDER.mkdirs();

      File testFile = new File(TestConstants.CURRENT_FOLDER, "ExampleInterfaceNoPackage.java");
      FileUtils.copyFile(new File("src/test/resources/sourceInstrumentation/ExampleInterfaceNoPackage.java"), testFile);
      // File testFile = SourceInstrumentationTestUtil.copyResource("Utils.java", "/sourceInstrumentation/");

      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.OPERATIONEXECUTION);
      instrumenter.instrument(testFile);

      testFileIsNotInstrumented(testFile, "public static java.util.Date com.test.Utils.utilMethod(java.lang.String)", "OperationExecutionRecord");
   }
   
   @Test
   public void testInterface() throws IOException {
      TestConstants.CURRENT_FOLDER.mkdirs();

      File testFile = new File(TestConstants.CURRENT_FOLDER, "ExampleInterface.java");
      FileUtils.copyFile(new File("src/test/resources/sourceInstrumentation/ExampleInterface.java"), testFile);
      // File testFile = SourceInstrumentationTestUtil.copyResource("Utils.java", "/sourceInstrumentation/");

      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.OPERATIONEXECUTION);
      instrumenter.instrument(testFile);

      testFileIsNotInstrumented(testFile, "public static java.util.Date com.test.Utils.utilMethod(java.lang.String)", "OperationExecutionRecord");
   }
   
   public static void testFileIsNotInstrumented(final File testFile, final String fqn, final String recordName) throws IOException {
      String changedSource = FileUtils.readFileToString(testFile, StandardCharsets.UTF_8);
      
      System.out.println(changedSource);

      Assert.assertThat(changedSource, Matchers.not(Matchers.containsString("import kieker.monitoring.core.controller.MonitoringController;")));
      Assert.assertThat(changedSource, Matchers.not(Matchers.containsString("import kieker.monitoring.core.registry.ControlFlowRegistry;")));
      Assert.assertThat(changedSource, Matchers.not(Matchers.containsString("import kieker.monitoring.core.registry.SessionRegistry;")));

      Assert.assertThat(changedSource, Matchers.not(Matchers.containsString("signature = \"" + fqn)));
      Assert.assertThat(changedSource, Matchers.not(Matchers.containsString("new " + recordName)));
   }
}
