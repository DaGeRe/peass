package de.dagere.peass.dependency.execution.gradle;

import de.dagere.peass.TestConstants;
import de.dagere.peass.TestUtil;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.gradle.GradleBuildfileEditor;
import de.dagere.peass.execution.gradle.GradleTaskAnalyzer;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class TestBuildGradleSystemProperties {

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
   public void testEditSystemProperties() throws IOException {
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "buildSystemProperties.gradle");

      final File destFile = GradleTestUtil.initProject(gradleFile, TestBuildGradle.CURRENT);

      GradleTaskAnalyzer taskAnalyzerMock = new GradleTaskAnalyzer(TestBuildGradle.CURRENT, new EnvironmentVariables());
      
      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, taskAnalyzerMock);
      editor.addDependencies(new File("xyz"), new EnvironmentVariables());

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.defaultCharset());
      System.out.println(gradleFileContents);

      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("'junit.jupiter.execution.parallel.mode.default'             , 'SAME_THREAD'"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("'junit.jupiter.execution.parallel.enabled'             : 'false'"));
   }
}
