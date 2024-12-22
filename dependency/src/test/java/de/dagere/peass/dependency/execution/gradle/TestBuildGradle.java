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
import de.dagere.peass.execution.maven.pom.MavenPomUtil;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.testtransformation.JUnitVersions;

public class TestBuildGradle {

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
   }

   @Test
   public void testNoUpdate() throws IOException {
      final File gradleFile = new File(GRADLE_BUILDFILE_FOLDER, "differentPlugin.gradle");

      final File destFile = GradleTestUtil.initProject(gradleFile, CURRENT);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(CURRENT));
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());

      Assert.assertTrue(FileUtils.contentEquals(gradleFile, destFile));
   }

   @Test
   public void testSprintBootUpdate() throws IOException {
      final File gradleFile = new File(GRADLE_BUILDFILE_FOLDER, "build_boot_oldVersion.gradle");

      final String gradleFileContents = updateGradleFile(gradleFile);

      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("de.dagere.kopeme:kopeme-junit"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("ext['junit-jupiter.version']='" + MavenPomUtil.JUPITER_VERSION + "'"));
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
   public void testJVMArgs() throws IOException {
      final File gradleFile = new File(GRADLE_BUILDFILE_FOLDER, "buildJVMArgs.gradle");

      mockedTransformer.getConfig().getKiekerConfig().setOnlyOneCallRecording(true);

      final String gradleFileContents = updateGradleFile(gradleFile);

      int testIndex = gradleFileContents.indexOf("test {");
      int integrationTestIndex = gradleFileContents.indexOf("task integrationTest");
      
      String testTask = gradleFileContents.substring(testIndex, integrationTestIndex);
      Assert.assertEquals(1, StringUtils.countMatches(testTask, "jvmArgs"));
      
      String integrationTestTask = gradleFileContents.substring(integrationTestIndex);
      
      System.out.println(integrationTestTask);
      
      Assert.assertEquals(1, StringUtils.countMatches(integrationTestTask, "jvmArgs"));
   }
   
   

   @Test
   public void testIntegrationtest() throws IOException {
      final File gradleFile = new File(TestGradleBuildfileVisitor.GRADLE_FOLDER, "build-integrationtest.gradle");

      final String gradleFileContents = updateGradleFile(gradleFile);

      String integrationTestStart = gradleFileContents.substring(gradleFileContents.indexOf("tasks.register('integrationTest', Test)"));
      String integrationTestTask = integrationTestStart.substring(0, integrationTestStart.indexOf('}'));

      System.out.println(integrationTestTask);

      MatcherAssert.assertThat(integrationTestTask, Matchers.containsString("systemProperty \"kieker.monitoring.configuration\""));
   }

   @Test
   public void testIntegrationtestVariant2() throws IOException {
      final File gradleFile = new File(TestGradleBuildfileVisitor.GRADLE_FOLDER, "build-integrationtest2.gradle");

      final String gradleFileContents = updateGradleFile(gradleFile);

      String integrationTestStart = gradleFileContents.substring(gradleFileContents.indexOf("task integrationTest(type: Test)"));
      String integrationTestTask = integrationTestStart.substring(0, integrationTestStart.indexOf('}'));

      System.out.println(integrationTestTask);

      MatcherAssert.assertThat(integrationTestTask, Matchers.containsString("systemProperty \"kieker.monitoring.configuration\""));
   }

   public void testUpdate(final File gradleFile, final boolean buildtools) throws IOException {
      final String gradleFileContents = updateGradleFile(gradleFile);

      if (buildtools) {
         MatcherAssert.assertThat(gradleFileContents, Matchers.anyOf(Matchers.containsString("'buildTools': '19.1.0'"),
               Matchers.containsString("buildToolsVersion 19.1.0")));
      }

      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("de.dagere.kopeme:kopeme-junit"));
   }

   private String updateGradleFile(final File gradleFile) throws IOException {
      final File destFile = GradleTestUtil.initProject(gradleFile, CURRENT);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(CURRENT));
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.defaultCharset());
      return gradleFileContents;
   }
}
