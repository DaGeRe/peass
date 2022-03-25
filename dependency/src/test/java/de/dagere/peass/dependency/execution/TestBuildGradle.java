package de.dagere.peass.dependency.execution;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.execution.gradle.TestFindDependencyVisitor;
import de.dagere.peass.execution.gradle.GradleBuildfileEditor;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class TestBuildGradle {

   public static final File CURRENT = new File(new File("target"), "current_gradle");

   public static final File GRADLE_BUILDFILE_FOLDER = new File("src/test/resources/gradle");

   private JUnitTestTransformer mockedTransformer;

   @BeforeEach
   public void setupTransformer() {
      mockedTransformer = Mockito.mock(JUnitTestTransformer.class);
      MeasurementConfig config = new MeasurementConfig(2);
      config.setUseKieker(true);
      Mockito.when(mockedTransformer.getConfig()).thenReturn(config);
   }

   @Test
   public void testNoUpdate() throws IOException {
      final File gradleFile = new File(GRADLE_BUILDFILE_FOLDER, "differentPlugin.gradle");

      final File destFile = copyGradlefile(gradleFile);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(CURRENT));
      editor.addDependencies(new File("xyz"));

      Assert.assertTrue(FileUtils.contentEquals(gradleFile, destFile));
   }

   @Test
   public void testSprintBootUpdate() throws IOException {
      final File gradleFile = new File(GRADLE_BUILDFILE_FOLDER, "build_boot_oldVersion.gradle");

      final String gradleFileContents = updateGradleFile(gradleFile);

      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("de.dagere.kopeme:kopeme-junit"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("ext['junit-jupiter.version']='5.8.2'"));
   }

   @Test
   public void testExclusionRemoval() throws IOException {
      final File gradleFile = new File(GRADLE_BUILDFILE_FOLDER, "build_with_exclusions.gradle");

      final String gradleFileContents = updateGradleFile(gradleFile);

      MatcherAssert.assertThat(gradleFileContents, Matchers.not(Matchers.containsString("exclude group: 'junit', module: 'junit'")));
      MatcherAssert.assertThat(gradleFileContents, Matchers.not(Matchers.containsString("exclude group: 'org.junit.vintage', module: 'junit-vintage-engine'")));
   }
   
   @Test
   public void testExclusionRemovalFromDependencies() throws IOException {
      final File gradleFile = new File(GRADLE_BUILDFILE_FOLDER, "build_with_exclusions2.gradle");

      final String gradleFileContents = updateGradleFile(gradleFile);

      MatcherAssert.assertThat(gradleFileContents, Matchers.not(Matchers.containsString("exclude group: 'junit', module: 'junit'")));
      MatcherAssert.assertThat(gradleFileContents, Matchers.not(Matchers.containsString("exclude group: 'org.junit.vintage', module: 'junit-vintage-engine'")));
   }
   
   @Test
   public void testIntegrationtest() throws IOException {
      final File gradleFile = new File(TestFindDependencyVisitor.GRADLE_FOLDER, "build-integrationtest.gradle");

      final String gradleFileContents = updateGradleFile(gradleFile);

      String integrationTestStart = gradleFileContents.substring(gradleFileContents.indexOf("tasks.register('integrationTest', Test)"));
      String integrationTestTask = integrationTestStart.substring(0, integrationTestStart.indexOf('}'));
      
      System.out.println(integrationTestTask);
      
      MatcherAssert.assertThat(integrationTestTask, Matchers.containsString("systemProperty \"kieker.monitoring.configuration\""));
   }
   
   @Test
   public void testIntegrationtestVariant2() throws IOException {
      final File gradleFile = new File(TestFindDependencyVisitor.GRADLE_FOLDER, "build-integrationtest2.gradle");

      final String gradleFileContents = updateGradleFile(gradleFile);

      String integrationTestStart = gradleFileContents.substring(gradleFileContents.indexOf("task integrationTest(type: Test)"));
      String integrationTestTask = integrationTestStart.substring(0, integrationTestStart.indexOf('}'));
      
      System.out.println(integrationTestTask);
      
      MatcherAssert.assertThat(integrationTestTask, Matchers.containsString("systemProperty \"kieker.monitoring.configuration\""));
   }

   @Test
   public void testBuildtoolUpdate() throws IOException {
      final File gradleFile = new File(GRADLE_BUILDFILE_FOLDER, "build.gradle");
      testUpdate(gradleFile, true);

      final File gradleFile2 = new File(GRADLE_BUILDFILE_FOLDER, "v2.gradle");
      testUpdate(gradleFile2, true);
   }
   
   @Test
   public void testAndroidLib() throws IOException {
      final File gradleFile3 = new File(GRADLE_BUILDFILE_FOLDER, "androidlib.gradle");
      testUpdate(gradleFile3, false);
   }

   public void testUpdate(final File gradleFile, final boolean buildtools) throws IOException {
      final String gradleFileContents = updateGradleFile(gradleFile);

      if (buildtools) {
         MatcherAssert.assertThat(gradleFileContents, Matchers.anyOf(Matchers.containsString("'buildTools': '19.1.0'"),
               Matchers.containsString("buildToolsVersion 19.1.0")));
      }

      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("de.dagere.kopeme:kopeme-junit"));
   }

   public static File copyGradlefile(final File gradleFile) throws IOException {
      final File destFile = new File(CURRENT, "build.gradle");
      FileUtils.copyFile(gradleFile, destFile);
      return destFile;
   }
   
   private String updateGradleFile(final File gradleFile) throws IOException {
      final File destFile = copyGradlefile(gradleFile);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(CURRENT));
      editor.addDependencies(new File("xyz"));

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.defaultCharset());
      return gradleFileContents;
   }
}
