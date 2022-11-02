package de.dagere.peass.dependency.execution.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.gradle.GradleBuildfileEditor;
import de.dagere.peass.execution.gradle.GradleTaskAnalyzer;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.testtransformation.JUnitVersions;

public class TestJVMArgsGradle {

   public static final File CURRENT = new File(new File("target"), "current_gradle");

   public static final File GRADLE_BUILDFILE_FOLDER = new File("src/test/resources/gradle");

   private JUnitTestTransformer mockedTransformer;
   private GradleTaskAnalyzer taskAnalyzerMock;

   @BeforeEach
   public void setupTransformer() throws IOException {
      mockedTransformer = Mockito.mock(JUnitTestTransformer.class);
      MeasurementConfig config = new MeasurementConfig(2);
      config.getKiekerConfig().setUseKieker(true);
      Mockito.when(mockedTransformer.getConfig()).thenReturn(config);
      Mockito.when(mockedTransformer.getProjectFolder()).thenReturn(CURRENT);
      JUnitVersions junitVersions = new JUnitVersions();
      junitVersions.setJunit4(true);
      Mockito.when(mockedTransformer.getJUnitVersions()).thenReturn(junitVersions);

      if (CURRENT.exists()) {
         FileUtils.cleanDirectory(CURRENT);
      }

      mockedTransformer.getConfig().getKiekerConfig().setOnlyOneCallRecording(true);
      mockedTransformer.getConfig().getExecutionConfig().setXmx("5g");
      
      taskAnalyzerMock = Mockito.mock(GradleTaskAnalyzer.class);
      Mockito.when(taskAnalyzerMock.isUseJava()).thenReturn(true);
   }

   @Test
   public void testHeapsizeReplacement() throws IOException {
      final String[] testTasks = getTestTasks("build.gradle");
      final String testTask = testTasks[0];
      final String integrationTestTask = testTasks[1];

      Assert.assertEquals(1, StringUtils.countMatches(testTask, "maxHeapSize"));
      MatcherAssert.assertThat(testTask, Matchers.containsString("maxHeapSize = \"5g\""));
      Assert.assertFalse(checkJVMArgsContainWhitespace(testTask));

      Assert.assertEquals(1, StringUtils.countMatches(integrationTestTask, "jvmArgs"));
      MatcherAssert.assertThat(integrationTestTask, Matchers.containsString("maxHeapSize = \"5g\""));
      Assert.assertFalse(checkJVMArgsContainWhitespace(integrationTestTask));
   }

   @Test
   public void testXmxSetting() throws IOException {
      final String[] testTasks = getTestTasks("buildNoJVMArgs.gradle");
      final String testTask = testTasks[0];
      final String integrationTestTask = testTasks[1];

      Assert.assertEquals(1, StringUtils.countMatches(testTask, "jvmArgs"));
      MatcherAssert.assertThat(testTask, Matchers.containsString("-Xmx5g"));
      Assert.assertFalse(checkJVMArgsContainWhitespace(testTask));

      Assert.assertEquals(1, StringUtils.countMatches(integrationTestTask, "jvmArgs"));
      MatcherAssert.assertThat(integrationTestTask, Matchers.containsString("-Xmx5g"));
      Assert.assertFalse(checkJVMArgsContainWhitespace(integrationTestTask));
   }

   @Test
   public void testXmxIncreaseGigabyte() throws IOException {
      final String[] testTasks = getTestTasks("buildJVMArgs.gradle");
      final String testTask = testTasks[0];
      final String integrationTestTask = testTasks[1];

      Assert.assertEquals(1, StringUtils.countMatches(testTask, "jvmArgs"));
      MatcherAssert.assertThat(testTask, Matchers.containsString("-Xmx5g"));
      Assert.assertFalse(checkJVMArgsContainWhitespace(testTask));

      Assert.assertEquals(1, StringUtils.countMatches(integrationTestTask, "jvmArgs"));
      MatcherAssert.assertThat(integrationTestTask, Matchers.containsString("-Xmx5g"));
      Assert.assertFalse(checkJVMArgsContainWhitespace(integrationTestTask));
   }

   @Test
   public void testXmxIncreaseMegabyte() throws IOException {
      final String[] testTasks = getTestTasks("buildJVMArgsInMegabyte.gradle");
      final String testTask = testTasks[0];
      final String integrationTestTask = testTasks[1];

      Assert.assertEquals(1, StringUtils.countMatches(testTask, "jvmArgs"));
      MatcherAssert.assertThat(testTask, Matchers.containsString("-Xmx5g"));
      Assert.assertFalse(checkJVMArgsContainWhitespace(testTask));

      Assert.assertEquals(1, StringUtils.countMatches(integrationTestTask, "jvmArgs"));
      MatcherAssert.assertThat(integrationTestTask, Matchers.containsString("-Xmx5g"));
      Assert.assertFalse(checkJVMArgsContainWhitespace(integrationTestTask));
   }

   @Test
   public void testXmxIncreaseSimple() throws IOException {
      final String[] testTasks = getTestTasks("buildJVMArgsSimple.gradle");
      final String testTask = testTasks[0];
      final String integrationTestTask = testTasks[1];

      Assert.assertEquals(1, StringUtils.countMatches(testTask, "jvmArgs"));
      MatcherAssert.assertThat(testTask, Matchers.containsString("-Xmx5g"));
      Assert.assertFalse(checkJVMArgsContainWhitespace(testTask));

      Assert.assertEquals(1, StringUtils.countMatches(integrationTestTask, "jvmArgs"));
      MatcherAssert.assertThat(integrationTestTask, Matchers.containsString("-Xmx5g"));
      Assert.assertFalse(checkJVMArgsContainWhitespace(integrationTestTask));
   }

   @Test
   public void testXmxDoubleArgs() throws IOException {
      final String[] testTasks = getTestTasks("buildJVMDoubleArgs.gradle");
      final String testTask = testTasks[0];
      final String integrationTestTask = testTasks[1];

      Assert.assertEquals(1, StringUtils.countMatches(testTask, "jvmArgs"));
      MatcherAssert.assertThat(testTask, Matchers.containsString("-Xmx5g\'"));
      Assert.assertFalse(checkJVMArgsContainWhitespace(testTask));

      Assert.assertEquals(1, StringUtils.countMatches(integrationTestTask, "jvmArgs"));
      MatcherAssert.assertThat(integrationTestTask, Matchers.containsString("-Xmx5g\'"));
      Assert.assertFalse(checkJVMArgsContainWhitespace(integrationTestTask));
   }

   @Test
   public void testAspectJAddedWithOnlyOneCallRecordingAndEmptyTestBlocks() throws IOException {
      final String[] testTasks = getTestTasks("minimalGradleEmptyTestblocks.gradle");
      final String testTask = testTasks[0];
      final String integrationTestTask = testTasks[1];
      Assert.assertTrue(testTask.contains("jvmArgs=[") && testTask.contains("aspectj.jar"));
      Assert.assertTrue(integrationTestTask.contains("jvmArgs=[") && integrationTestTask.contains("aspectj.jar"));
   }

   @Test
   public void testAspectJAddedWithOnlyOneCallRecordingAndNoTestBlocks() throws IOException {
      final String[] testTasks = getTestTasks("minimalGradleNoTestblocks.gradle");
      final String testTask = testTasks[0];
      final String integrationTestTask = testTasks[1];
      Assert.assertTrue(testTask.contains("jvmArgs=[") && testTask.contains("aspectj.jar"));
      Assert.assertTrue(integrationTestTask.contains("jvmArgs=[") && integrationTestTask.contains("aspectj.jar"));
   }

   private String updateGradleFile(final File gradleFile) throws IOException {
      final File destFile = GradleTestUtil.initProject(gradleFile, CURRENT);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(CURRENT), taskAnalyzerMock);
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.defaultCharset());
      return gradleFileContents;
   }

   private String[] getTestTasks(final String gradleFileName) throws IOException {

      final File gradleBuildFile = new File(GRADLE_BUILDFILE_FOLDER, gradleFileName);
      final String gradleFileContents = updateGradleFile(gradleBuildFile);
      final int testIndex = gradleFileContents.indexOf("test {");
      int integrationTestIndex = gradleFileContents.indexOf("task integrationTest");

      /*
       * integrationTest maybe uses alternative syntax for registration
       */
      if (integrationTestIndex == -1) {
         integrationTestIndex = gradleFileContents.indexOf("tasks.register('integrationTest'");
      }

      if (integrationTestIndex != -1) {
         if (integrationTestIndex > testIndex) {
            return new String[] { gradleFileContents.substring(testIndex, integrationTestIndex), gradleFileContents.substring(integrationTestIndex) };
         } else {
            return new String[] {gradleFileContents.substring(testIndex), gradleFileContents.substring(integrationTestIndex, testIndex) };
         }

      } else {
         final String testTaskTillEOF = gradleFileContents.substring(testIndex);
         return new String[] { testTaskTillEOF.substring(0, testTaskTillEOF.lastIndexOf("}")), "" };
      }
   }

   private boolean checkJVMArgsContainWhitespace (final String task) {
      final String jvmArgsPattern = "jvmArgs=[";
      final int jvmArgsIndex = task.indexOf(jvmArgsPattern);
      final String jvmArguments = task.substring(jvmArgsIndex + jvmArgsPattern.length(), task.lastIndexOf("]"));

      return StringUtils.containsWhitespace(jvmArguments);
   }
}
