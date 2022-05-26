package de.dagere.peass.dependency.execution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import de.dagere.kopeme.parsing.GradleParseHelper;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.gradle.GradleTestExecutor;
import de.dagere.peass.execution.gradle.SettingsFileParser;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.vcs.VersionControlSystem;

public class TestGradleReplacement {
   
   private static final Logger LOG = LogManager.getLogger(TestGradleReplacement.class);

   private static final String ALTERNATIVE_FILE_CONTENT = "This is an alternative buildfile";

   @TempDir
   File tempDir;
   
   File projectDir;
   
   @BeforeEach
   public void prepareFolder() throws IOException {
      FileUtils.deleteDirectory(tempDir);
      
      projectDir = new File(tempDir, "project");

      File src = new File("src/test/resources/gradle-multimodule-example");
      FileUtils.copyDirectory(src, projectDir);
      GradleTestUtil.initWrapper(projectDir);

      List<File> modules = SettingsFileParser.getModules(projectDir).getModules();
      for (File module : modules) {
         File alternativeGradle = new File(module, GradleParseHelper.ALTERNATIVE_NAME);
         LOG.debug("Creating alternative buildfile: {}", alternativeGradle);
         try (BufferedWriter writer = new BufferedWriter(new FileWriter(alternativeGradle))) {
            writer.write(ALTERNATIVE_FILE_CONTENT);
         }
      }
   }

   @Test
   public void testReplacement() throws IOException, XmlPullParserException, InterruptedException {
      prepareBuildfiles(true);

      List<File> modules = SettingsFileParser.getModules(projectDir).getModules();
      for (File module : modules) {
         final File gradleFile = GradleParseHelper.findGradleFile(module);
         String fileContent = FileUtils.readFileToString(gradleFile, StandardCharsets.UTF_8);
         MatcherAssert.assertThat(fileContent, Matchers.containsString(ALTERNATIVE_FILE_CONTENT));
      }
   }

   @Test
   public void testNoReplacement() throws IOException, XmlPullParserException, InterruptedException {
      prepareBuildfiles(false);

      List<File> modules = SettingsFileParser.getModules(projectDir).getModules();
      for (File module : modules) {
         final File gradleFile = GradleParseHelper.findGradleFile(module);
         String fileContent = FileUtils.readFileToString(gradleFile, StandardCharsets.UTF_8);
         MatcherAssert.assertThat(fileContent, Matchers.not(Matchers.containsString(ALTERNATIVE_FILE_CONTENT)));
      }
   }

   private void prepareBuildfiles(final boolean replace) throws IOException, XmlPullParserException, InterruptedException {
      JUnitTestTransformer mockedTransformer = Mockito.mock(JUnitTestTransformer.class);
      MeasurementConfig config = new MeasurementConfig(2);

      config.getExecutionConfig().setUseAlternativeBuildfile(replace);
      Mockito.when(mockedTransformer.getConfig()).thenReturn(config);

      PeassFolders folders;
      try (MockedStatic<VersionControlSystem> utilities = Mockito.mockStatic(VersionControlSystem.class)) {
         utilities.when(() -> VersionControlSystem.getVersionControlSystem(Mockito.any())).thenReturn(VersionControlSystem.GIT);
         folders = new PeassFolders(projectDir);
     }
      Mockito.when(mockedTransformer.getProjectFolder()).thenReturn(projectDir);

      GradleTestExecutor gte = new GradleTestExecutor(folders, mockedTransformer, new EnvironmentVariables());

      gte.prepareKoPeMeExecution(new File(projectDir, "out.txt"));
   }

}
