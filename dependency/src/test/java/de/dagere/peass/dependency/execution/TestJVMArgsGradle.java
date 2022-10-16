package de.dagere.peass.dependency.execution;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.gradle.GradleBuildfileEditor;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.testtransformation.JUnitVersions;

public class TestJVMArgsGradle {
   
   public static final File CURRENT = new File(new File("target"), "current_gradle");

   public static final File GRADLE_BUILDFILE_FOLDER = new File("src/test/resources/gradle");

   private JUnitTestTransformer mockedTransformer;

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
   }
   
   @Ignore
   @Test
   public void testHeapsizeReplacement() throws IOException {
      final File gradleFile = new File(GRADLE_BUILDFILE_FOLDER, "build.gradle");

      final String gradleFileContents = updateGradleFile(gradleFile);

      int testIndex = gradleFileContents.indexOf("test {");
      int integrationTestIndex = gradleFileContents.indexOf("task integrationTest");
      
      String testTask = gradleFileContents.substring(testIndex, integrationTestIndex);
      
      System.out.println(gradleFileContents);
      
      Assert.assertEquals(1, StringUtils.countMatches(testTask, "maxHeapSize"));
      MatcherAssert.assertThat(testTask, Matchers.containsString("maxHeapSize = \"5g\""));
      
      String integrationTestTask = gradleFileContents.substring(integrationTestIndex);
      
      Assert.assertEquals(1, StringUtils.countMatches(integrationTestTask, "jvmArgs"));
      MatcherAssert.assertThat(integrationTestTask, Matchers.containsString("-Xmx5g"));
   }
   
   @Test
   public void testXmxSetting() throws IOException {
      final File gradleFile = new File(GRADLE_BUILDFILE_FOLDER, "buildNoJVMArgs.gradle");

      final String gradleFileContents = updateGradleFile(gradleFile);

      int testIndex = gradleFileContents.indexOf("test {");
      int integrationTestIndex = gradleFileContents.indexOf("task integrationTest");
      
      String testTask = gradleFileContents.substring(testIndex, integrationTestIndex);
      
      System.out.println(gradleFileContents);
      
      Assert.assertEquals(1, StringUtils.countMatches(testTask, "jvmArgs"));
      MatcherAssert.assertThat(testTask, Matchers.containsString("-Xmx5g"));
      
      String integrationTestTask = gradleFileContents.substring(integrationTestIndex);
      
      Assert.assertEquals(1, StringUtils.countMatches(integrationTestTask, "jvmArgs"));
      MatcherAssert.assertThat(integrationTestTask, Matchers.containsString("-Xmx5g"));
   }
   
   @Test
   public void testXmxIncreaseGigabyte() throws IOException {
      final File gradleFile = new File(GRADLE_BUILDFILE_FOLDER, "buildJVMArgs.gradle");

      final String gradleFileContents = updateGradleFile(gradleFile);

      int testIndex = gradleFileContents.indexOf("test {");
      int integrationTestIndex = gradleFileContents.indexOf("task integrationTest");
      
      String testTask = gradleFileContents.substring(testIndex, integrationTestIndex);
      Assert.assertEquals(1, StringUtils.countMatches(testTask, "jvmArgs"));
      MatcherAssert.assertThat(testTask, Matchers.containsString("-Xmx5g"));
      
      String integrationTestTask = gradleFileContents.substring(integrationTestIndex);
      
      Assert.assertEquals(1, StringUtils.countMatches(integrationTestTask, "jvmArgs"));
      MatcherAssert.assertThat(integrationTestTask, Matchers.containsString("-Xmx5g"));
   }
   
   @Test
   public void testXmxIncreaseMegabyte() throws IOException {
      final File gradleFile = new File(GRADLE_BUILDFILE_FOLDER, "buildJVMArgsInMegabyte.gradle");

      final String gradleFileContents = updateGradleFile(gradleFile);

      int testIndex = gradleFileContents.indexOf("test {");
      int integrationTestIndex = gradleFileContents.indexOf("task integrationTest");
      
      String testTask = gradleFileContents.substring(testIndex, integrationTestIndex);
      Assert.assertEquals(1, StringUtils.countMatches(testTask, "jvmArgs"));
      MatcherAssert.assertThat(testTask, Matchers.containsString("-Xmx5g"));
      
      String integrationTestTask = gradleFileContents.substring(integrationTestIndex);
      
      Assert.assertEquals(1, StringUtils.countMatches(integrationTestTask, "jvmArgs"));
      MatcherAssert.assertThat(integrationTestTask, Matchers.containsString("-Xmx5g"));
   }
   
   private String updateGradleFile(final File gradleFile) throws IOException {
      final File destFile = GradleTestUtil.initProject(gradleFile, CURRENT);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(CURRENT));
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.defaultCharset());
      return gradleFileContents;
   }
}
