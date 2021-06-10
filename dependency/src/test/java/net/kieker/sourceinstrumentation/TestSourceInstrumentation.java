package net.kieker.sourceinstrumentation;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import de.dagere.peass.TestConstants;
import net.kieker.sourceinstrumentation.instrument.InstrumentKiekerSource;

public class TestSourceInstrumentation {

   @Test
   public void testSingleClass() throws IOException {
      TestConstants.CURRENT_FOLDER.mkdirs();

      File testFile = SourceInstrumentationTestUtil.copyResource("src/main/java/de/peass/C0_0.java", "/sourceInstrumentation/project_2/");

      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.OPERATIONEXECUTION);
      instrumenter.instrument(testFile);

      testFileIsInstrumented(testFile, "public void de.peass.C0_0.method0()", "OperationExecutionRecord");
   }
   
   @Test
   public void testUtilClass() throws IOException {
      TestConstants.CURRENT_FOLDER.mkdirs();

      File testFile = new File(TestConstants.CURRENT_FOLDER, "Utils.java");
      FileUtils.copyFile(new File("src/test/resources/sourceInstrumentation/Utils.java"), testFile);
      // File testFile = SourceInstrumentationTestUtil.copyResource("Utils.java", "/sourceInstrumentation/");

      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.OPERATIONEXECUTION);
      instrumenter.instrument(testFile);

      testFileIsInstrumented(testFile, "public static java.util.Date com.test.Utils.utilMethod(java.lang.String)", "OperationExecutionRecord");
   }

   @Test
   public void testGenericsClass() throws IOException {
      TestConstants.CURRENT_FOLDER.mkdirs();

      File testFile = SourceInstrumentationTestUtil.copyResource("src/main/java/de/peass/C0_0.java", "/sourceInstrumentation/project_2_complex/");

      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.OPERATIONEXECUTION);
      instrumenter.instrument(testFile);

      testFileIsInstrumented(testFile, "public java.util.Collection de.peass.C0_0.myCollection(java.util.List)", "OperationExecutionRecord");
      testFileIsInstrumented(testFile, "public java.util.Collection[] de.peass.C0_0.myCollection2(java.util.List)", "OperationExecutionRecord");
   }
   
   @Test
   public void testNoPackageClass() throws IOException {
      TestConstants.CURRENT_FOLDER.mkdirs();

      File testFile = SourceInstrumentationTestUtil.copyResource("src/main/java/NoPackageExample.java", "/sourceInstrumentation/project_2_complex/");

      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.OPERATIONEXECUTION);
      instrumenter.instrument(testFile);

      testFileIsInstrumented(testFile, "public ReturnType NoPackageExample.newStuff", "OperationExecutionRecord");
   }
   
   public static void testFileIsInstrumented(final File testFile, final String fqn, final String recordName) throws IOException {
      String changedSource = FileUtils.readFileToString(testFile, StandardCharsets.UTF_8);

      MatcherAssert.assertThat(changedSource, Matchers.containsString("import kieker.monitoring.core.controller.MonitoringController;"));
      MatcherAssert.assertThat(changedSource, Matchers.containsString("import kieker.monitoring.core.registry.ControlFlowRegistry;"));
      MatcherAssert.assertThat(changedSource, Matchers.containsString("import kieker.monitoring.core.registry.SessionRegistry;"));

      MatcherAssert.assertThat(changedSource, Matchers.containsString("signature = \"" + fqn));
      MatcherAssert.assertThat(changedSource, Matchers.containsString("new " + recordName));
   }

   @Test
   public void testInnerConstructor() throws IOException {
      SourceInstrumentationTestUtil.initSimpleProject("/sourceInstrumentation/example_instanceInnerClass/");

      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.OPERATIONEXECUTION);
      instrumenter.instrumentProject(TestConstants.CURRENT_FOLDER);

      testFileIsInstrumented(new File(TestConstants.CURRENT_FOLDER, "src/main/java/de/peass/C0_0.java"), "public new de.peass.C0_0$InstanceInnerClass.<init>(de.peass.C0_0,int)",
            "OperationExecutionRecord");
      testFileIsInstrumented(new File(TestConstants.CURRENT_FOLDER, "src/main/java/de/peass/C0_0.java"),
            "new de.peass.C0_0$InstanceInnerClass$InnerInnerClass.<init>(de.peass.C0_0$InstanceInnerClass)", "OperationExecutionRecord");

   }

   @Test
   public void testProjectInstrumentation() throws IOException {
      SourceInstrumentationTestUtil.initProject("/sourceInstrumentation/project_2/");

      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.OPERATIONEXECUTION);
      instrumenter.instrumentProject(TestConstants.CURRENT_FOLDER);

      testFileIsInstrumented(new File(TestConstants.CURRENT_FOLDER, "src/main/java/de/peass/C0_0.java"), "public void de.peass.C0_0.method0()", "OperationExecutionRecord");
      testFileIsInstrumented(new File(TestConstants.CURRENT_FOLDER, "src/main/java/de/peass/C1_0.java"), "public void de.peass.C1_0.method0()", "OperationExecutionRecord");
      testFileIsInstrumented(new File(TestConstants.CURRENT_FOLDER, "src/main/java/de/peass/AddRandomNumbers.java"), "public int de.peass.AddRandomNumbers.getValue()",
            "OperationExecutionRecord");
      testFileIsInstrumented(new File(TestConstants.CURRENT_FOLDER, "/src/test/java/de/peass/MainTest.java"), "public void de.peass.MainTest.testMe()", "OperationExecutionRecord");
      testFileIsInstrumented(new File(TestConstants.CURRENT_FOLDER, "/src/test/java/de/peass/MainTest.java"), "public new de.peass.MainTest.<init>()", "OperationExecutionRecord");

      testConstructorVisibility();
   }

   private void testConstructorVisibility() throws IOException {
      String changedSourceC1 = FileUtils.readFileToString(new File(TestConstants.CURRENT_FOLDER, "src/main/java/de/peass/C1_0.java"), StandardCharsets.UTF_8);
      MatcherAssert.assertThat(changedSourceC1, Matchers.containsString("String " + InstrumentationConstants.PREFIX + "signature = \"public new de.peass.C1_0.<init>()\""));
      String changedSourceC0 = FileUtils.readFileToString(new File(TestConstants.CURRENT_FOLDER, "src/main/java/de/peass/C0_0.java"), StandardCharsets.UTF_8);
      MatcherAssert.assertThat(changedSourceC0, Matchers.containsString("String " + InstrumentationConstants.PREFIX + "signature = \"new de.peass.C0_0.<init>()\""));
   }

   @Test
   public void testDifferentSignatures() throws IOException {
      SourceInstrumentationTestUtil.initProject("/sourceInstrumentation/project_2_signatures/");

      InstrumentKiekerSource instrumenter = new InstrumentKiekerSource(AllowedKiekerRecord.REDUCED_OPERATIONEXECUTION);
      instrumenter.instrumentProject(TestConstants.CURRENT_FOLDER);

      String changedSource = FileUtils.readFileToString(new File(TestConstants.CURRENT_FOLDER, "src/main/java/de/peass/C0_0.java"), StandardCharsets.UTF_8);

      MatcherAssert.assertThat(changedSource, Matchers.containsString("signature = \"public void de.peass.C0_0.method0(int)\""));
      MatcherAssert.assertThat(changedSource, Matchers.containsString("signature = \"public java.lang.String de.peass.C0_0.method0(java.lang.String)\""));
      MatcherAssert.assertThat(changedSource, Matchers.containsString("signature = \"public java.lang.String[] de.peass.C0_0.methodWithArrayParam(byte[],int[],java.lang.String[])\""));
      MatcherAssert.assertThat(changedSource, Matchers.containsString("signature = \"public java.util.List[] de.peass.C0_0.secondMethodWithArrayParam(byte[],int[],java.util.List[])\""));
      MatcherAssert.assertThat(changedSource, Matchers.containsString("signature = \"public de.peass.C0_0 de.peass.C0_0.doSomethingWithSamePackageObject(de.peass.C1_0)\""));
      MatcherAssert.assertThat(changedSource, Matchers.containsString("signature = \"public de.peass.C0_0$MyInnerClass2 de.peass.C0_0.getInnerInstance()\""));
      MatcherAssert.assertThat(changedSource, Matchers.containsString("signature = \"public static void de.peass.C0_0.myStaticStuff()\""));
      MatcherAssert.assertThat(changedSource, Matchers.containsString("signature = \"public new de.peass.C0_0$MyInnerClass.<init>(int)\""));
      MatcherAssert.assertThat(changedSource, Matchers.containsString("signature = \"public void de.peass.C0_0$MyInnerClass.innerMethod()\""));
   }
}
