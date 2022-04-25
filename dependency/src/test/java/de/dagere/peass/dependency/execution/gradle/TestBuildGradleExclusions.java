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

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.execution.TestBuildGradle;
import de.dagere.peass.execution.gradle.GradleBuildfileEditor;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class TestBuildGradleExclusions {

   private JUnitTestTransformer mockedTransformer;

   @BeforeEach
   public void setupTransformer() {
      mockedTransformer = Mockito.mock(JUnitTestTransformer.class);
      MeasurementConfig config = new MeasurementConfig(2);
      config.setUseKieker(true);
      Mockito.when(mockedTransformer.getConfig()).thenReturn(config);
   }

   @Test
   public void testLog4jSlf4jImplExclusion() throws IOException {
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "build.gradle");

      final File destFile = TestBuildGradle.copyGradlefile(gradleFile);
      mockedTransformer.getConfig().getExecutionConfig().setExcludeLog4jSlf4jImpl(true);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(TestBuildGradle.CURRENT));
      editor.addDependencies(new File("xyz"));

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("'de.dagere.kopeme:kopeme-junit"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("exclude group: 'org.apache.logging.log4j', module: 'log4j-slf4j-impl'"));
   }

   @Test
   public void testLog4jSlf4jImplExclusionWithConstraints() throws IOException {
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "buildConstraints.gradle");

      final File destFile = TestBuildGradle.copyGradlefile(gradleFile);
      mockedTransformer.getConfig().getExecutionConfig().setExcludeLog4jSlf4jImpl(true);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(TestBuildGradle.CURRENT));
      editor.addDependencies(new File("xyz"));

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

      final File destFile = TestBuildGradle.copyGradlefile(gradleFile);
      mockedTransformer.getConfig().getExecutionConfig().setExcludeLog4jToSlf4j(true);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(TestBuildGradle.CURRENT));
      editor.addDependencies(new File("xyz"));

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("'de.dagere.kopeme:kopeme-junit"));
      
      String configurationsString = gradleFileContents.substring(gradleFileContents.indexOf("configuration"), gradleFileContents.indexOf("configurations") + 100);
      
      System.out.println(configurationsString);
      
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("exclude group: 'org.apache.logging.log4j', module: 'log4j-to-slf4j'"));
   }
   
   
   @Test
   public void testAlreadyExistingSystemProperties() throws IOException {
      final File gradleFile = new File(TestBuildGradle.GRADLE_BUILDFILE_FOLDER, "buildProperties.gradle");

      final File destFile = TestBuildGradle.copyGradlefile(gradleFile);

      GradleBuildfileEditor editor = new GradleBuildfileEditor(mockedTransformer, destFile, new ProjectModules(TestBuildGradle.CURRENT));
      editor.addDependencies(new File("xyz"));

      final String gradleFileContents = FileUtils.readFileToString(destFile, Charset.defaultCharset());

      System.out.println(gradleFileContents);
      
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("'de.dagere.kopeme:kopeme-junit"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("kieker.monitoring.configuration"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.containsString("java.io.tmpdir"));
      MatcherAssert.assertThat(gradleFileContents, Matchers.not(Matchers.containsString("systemProperty")));
      
   }

}
