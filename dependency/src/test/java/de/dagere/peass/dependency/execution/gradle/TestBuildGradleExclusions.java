package de.dagere.peass.dependency.execution.gradle;

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

import de.dagere.peass.TestConstants;
import de.dagere.peass.TestUtil;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.gradle.GradleBuildfileEditor;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class TestBuildGradleExclusions {

   private JUnitTestTransformer mockedTransformer;

   @BeforeEach
   public void setupTransformer() {
      mockedTransformer = Mockito.mock(JUnitTestTransformer.class);
      MeasurementConfig config = new MeasurementConfig(2);
      config.getKiekerConfig().setUseKieker(true);
      Mockito.when(mockedTransformer.getConfig()).thenReturn(config);
      Mockito.when(mockedTransformer.getProjectFolder()).thenReturn(TestBuildGradle.CURRENT);
      Mockito.when(mockedTransformer.getJUnitVersions()).thenReturn(TestConstants.TEST_JUNIT_VERSIONS);
      
      TestUtil.deleteContents(TestBuildGradle.CURRENT);
   }

   @Test
   public void testLog4jSlf4jImplExclusion() throws IOException {
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "build.gradle");

      final File destFile = GradleTestUtil.initProject(gradleFile, TestBuildGradle.CURRENT);
      mockedTransformer.getConfig().getExecutionConfig().setExcludeLog4jSlf4jImpl(true);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(TestBuildGradle.CURRENT));
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("'de.dagere.kopeme:kopeme-junit"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("exclude group: 'org.apache.logging.log4j', module: 'log4j-slf4j-impl'"));
   }

   @Test
   public void testLog4jSlf4jImplExclusionWithConstraints() throws IOException {
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "buildConstraints.gradle");

      final File destFile = GradleTestUtil.initProject(gradleFile, TestBuildGradle.CURRENT);
      mockedTransformer.getConfig().getExecutionConfig().setExcludeLog4jSlf4jImpl(true);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(TestBuildGradle.CURRENT));
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      String dependencyString = gradleFileContents.substring(gradleFileContents.indexOf("dependencies"), gradleFileContents.indexOf("dependencies") + 500);

      System.out.println(dependencyString);

      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("'de.dagere.kopeme:kopeme-junit"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("') { exclude group: 'org.apache.logging.log4j', module: 'log4j-slf4j-impl' }"));
      String excludeString = "{ exclude group: 'org.apache.logging.log4j', module: 'log4j-slf4j-impl' }";
      String closingParanthesisConstraints = "        }";
      Assert.assertTrue(dependencyString.indexOf(excludeString) > dependencyString.indexOf(closingParanthesisConstraints));
   }

   @Test
   public void testLog4jToSlf4jExclusion() throws IOException {
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "buildConstraints.gradle");

      final File destFile = GradleTestUtil.initProject(gradleFile, TestBuildGradle.CURRENT);
      mockedTransformer.getConfig().getExecutionConfig().setExcludeLog4jToSlf4j(true);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(TestBuildGradle.CURRENT));
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("'de.dagere.kopeme:kopeme-junit"));
      
      String configurationsString = gradleFileContents.substring(gradleFileContents.indexOf("configuration"), gradleFileContents.indexOf("configurations") + 100);
      
      System.out.println(configurationsString);
      
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("exclude group: 'org.apache.logging.log4j', module: 'log4j-to-slf4j'"));
   }
   
   
   @Test
   public void testAlreadyExistingSystemProperties() throws IOException {
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "buildProperties.gradle");

      final File destFile = GradleTestUtil.initProject(gradleFile, TestBuildGradle.CURRENT);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(TestBuildGradle.CURRENT));
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      System.out.println(gradleFileContents);
      
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("'de.dagere.kopeme:kopeme-junit"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("kieker.monitoring.configuration"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("java.io.tmpdir"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.not(Matchers.containsString("systemProperty")));
      
   }
   
   @Test
   public void testAnboxEditing() throws IOException {
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "androidlib.gradle");

      final File destFile = GradleTestUtil.initProject(gradleFile, TestBuildGradle.CURRENT);
      
      mockedTransformer.getConfig().getExecutionConfig().setUseAnbox(true);
      
      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(TestBuildGradle.CURRENT));
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      System.out.println(gradleFileContents);
      
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("androidTestImplementation 'androidx.test:rules:1.4.0'"));
      
   }

   @Test
   public void testConflictingFileExclusion() throws IOException {
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "androidlib.gradle");

      final File destFile = GradleTestUtil.initProject(gradleFile, TestBuildGradle.CURRENT);

      mockedTransformer.getConfig().getExecutionConfig().setUseAnbox(true);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(TestBuildGradle.CURRENT));
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());
      
      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("'META-INF/DEPENDENCIES'"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("'META-INF/LICENSE.md'"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("'META-INF/NOTICE.md'"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("'META-INF/jing-copying.html'"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("'META-INF/LICENSE-notice.md'"));
   }

   @Test
   public void testAndroidVersions() throws IOException {
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "androidlib.gradle");

      final File destFile = GradleTestUtil.initProject(gradleFile, TestBuildGradle.CURRENT);

      mockedTransformer.getConfig().getExecutionConfig().setUseAnbox(true);

      final String gradleFileContentsBefore = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      // this is the original version in androidlib.gradle but should be replaced
      MatcherAssert.assertThat(gradleFileContentsBefore, Matchers.containsString("compileSdkVersion 19"));

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(TestBuildGradle.CURRENT));
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());

      final String gradleFileContentsAfter = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      System.out.println(gradleFileContentsAfter);

      // should be in the file anymore
      MatcherAssert.assertThat(gradleFileContentsAfter, Matchers.not(Matchers.containsString("compileSdkVersion 19")));

      MatcherAssert.assertThat(gradleFileContentsAfter, Matchers.containsString("compileSdkVersion 29"));
      MatcherAssert.assertThat(gradleFileContentsAfter, Matchers.containsString("minSdkVersion 26"));
      MatcherAssert.assertThat(gradleFileContentsAfter, Matchers.containsString("targetSdkVersion 29"));
   }

   @Test
   public void testMultidexEnabled() throws IOException {
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "androidlib.gradle");

      final File destFile = GradleTestUtil.initProject(gradleFile, TestBuildGradle.CURRENT);

      mockedTransformer.getConfig().getExecutionConfig().setUseAnbox(true);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(TestBuildGradle.CURRENT));
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("multiDexEnabled = true"));
   }
}
