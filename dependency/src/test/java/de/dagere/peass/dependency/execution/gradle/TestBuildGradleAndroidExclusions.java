package de.dagere.peass.dependency.execution.gradle;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assume;
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

public class TestBuildGradleAndroidExclusions {
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
   public void testAnboxEditing() throws IOException {
      Assume.assumeFalse(EnvironmentVariables.getVersion() == 8);
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
   public void testKopemeExclusionsForAndroid() throws IOException {
      Assume.assumeFalse(EnvironmentVariables.getVersion() == 8);
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "androidlib.gradle");

      final File destFile = GradleTestUtil.initProject(gradleFile, TestBuildGradle.CURRENT);

      mockedTransformer.getConfig().getExecutionConfig().setUseAnbox(true);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(TestBuildGradle.CURRENT));
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("exclude group: 'net.kieker-monitoring', module: 'kieker'"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("exclude group: 'org.hamcrest', module: 'hamcrest'"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("exclude group: 'org.aspectj', module: 'aspectjrt'"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("exclude group: 'org.aspectj', module: 'aspectjweaver'"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("exclude group: 'org.apache.logging.log4j', module: 'log4j-core'"));
   }

   @Test
   public void testConflictingFileExclusion() throws IOException {
      Assume.assumeFalse(EnvironmentVariables.getVersion() == 8);
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
      Assume.assumeFalse(EnvironmentVariables.getVersion() == 8);
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "androidlib.gradle");

      final File destFile = GradleTestUtil.initProject(gradleFile, TestBuildGradle.CURRENT);

      mockedTransformer.getConfig().getExecutionConfig().setUseAnbox(true);
      mockedTransformer.getConfig().getExecutionConfig().setAndroidCompileSdkVersion("31");
      mockedTransformer.getConfig().getExecutionConfig().setAndroidMinSdkVersion("27");
      mockedTransformer.getConfig().getExecutionConfig().setAndroidTargetSdkVersion("30");

      final String gradleFileContentsBefore = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      // this is the original version in androidlib.gradle but should be replaced
      MatcherAssert.assertThat(gradleFileContentsBefore, Matchers.containsString("compileSdkVersion 19"));

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(TestBuildGradle.CURRENT));
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());

      final String gradleFileContentsAfter = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      System.out.println(gradleFileContentsAfter);

      // should not be in the file anymore
      MatcherAssert.assertThat(gradleFileContentsAfter, Matchers.not(Matchers.containsString("compileSdkVersion 19")));

      MatcherAssert.assertThat(gradleFileContentsAfter, Matchers.containsString("compileSdkVersion 31"));
      MatcherAssert.assertThat(gradleFileContentsAfter, Matchers.containsString("minSdkVersion 27"));
      MatcherAssert.assertThat(gradleFileContentsAfter, Matchers.containsString("targetSdkVersion 30"));
   }

   @Test
   public void testMultidexEnabled() throws IOException {
      Assume.assumeFalse(EnvironmentVariables.getVersion() == 8);
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "androidlib.gradle");

      final File destFile = GradleTestUtil.initProject(gradleFile, TestBuildGradle.CURRENT);

      mockedTransformer.getConfig().getExecutionConfig().setUseAnbox(true);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(TestBuildGradle.CURRENT));
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("multiDexEnabled = true"));
   }

   @Test
   public void testJavaCompatibility() throws IOException {
      Assume.assumeFalse(EnvironmentVariables.getVersion() == 8);
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "androidlib.gradle");

      final File destFile = GradleTestUtil.initProject(gradleFile, TestBuildGradle.CURRENT);

      mockedTransformer.getConfig().getExecutionConfig().setUseAnbox(true);

      final String gradleFileContentsBefore = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      MatcherAssert.assertThat(gradleFileContentsBefore, Matchers.not(Matchers.containsString("sourceCompatibility JavaVersion.VERSION_1_8")));
      MatcherAssert.assertThat(gradleFileContentsBefore, Matchers.not(Matchers.containsString("targetCompatibility JavaVersion.VERSION_1_8")));

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(TestBuildGradle.CURRENT));
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());

      final String gradleFileContentsAfter = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      System.out.println(gradleFileContentsAfter);

      MatcherAssert.assertThat(gradleFileContentsAfter, Matchers.containsString("sourceCompatibility JavaVersion.VERSION_1_8"));
      MatcherAssert.assertThat(gradleFileContentsAfter, Matchers.containsString("targetCompatibility JavaVersion.VERSION_1_8"));
   }

   @Test
   public void testGradleVersionUpdate() throws IOException {
      Assume.assumeFalse(EnvironmentVariables.getVersion() == 8);
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "androidlib.gradle");

      final File destFile = GradleTestUtil.initProject(gradleFile, TestBuildGradle.CURRENT);

      final String GRADLE_VERSION = "4.2.2";

      mockedTransformer.getConfig().getExecutionConfig().setUseAnbox(true);
      mockedTransformer.getConfig().getExecutionConfig().setAndroidGradleVersion(GRADLE_VERSION);

      final String gradleFileContentsBefore = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      MatcherAssert.assertThat(gradleFileContentsBefore, Matchers.containsString("com.android.tools.build:gradle:7.4.2"));
      MatcherAssert.assertThat(gradleFileContentsBefore, Matchers.not(Matchers.containsString("com.android.tools.build:gradle:" + GRADLE_VERSION)));

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(TestBuildGradle.CURRENT));
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());

      final String gradleFileContentsAfter = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      System.out.println(gradleFileContentsAfter);

      MatcherAssert.assertThat(gradleFileContentsAfter, Matchers.not(Matchers.containsString("com.android.tools.build:gradle:7.4.2")));
      MatcherAssert.assertThat(gradleFileContentsAfter, Matchers.containsString("com.android.tools.build:gradle:" + GRADLE_VERSION));
   }
}
