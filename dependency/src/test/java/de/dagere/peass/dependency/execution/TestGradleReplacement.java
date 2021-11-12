package de.dagere.peass.dependency.execution;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.kopeme.parsing.GradleParseHelper;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class TestGradleReplacement {

   private static final String ALTERNATIVE_FILE_CONTENT = "This is an alternative buildfile";

   @BeforeEach
   public void prepareFolder() throws IOException {
      FileUtils.deleteDirectory(TestBuildGradle.CURRENT);
      FileUtils.deleteDirectory(new File(TestBuildGradle.CURRENT.getParentFile(), TestBuildGradle.CURRENT.getName() +"_peass"));
      
      File src = new File("src/test/resources/gradle-multimodule-example");
      FileUtils.copyDirectory(src, TestBuildGradle.CURRENT);
      List<File> modules = GradleParseUtil.getModules(TestBuildGradle.CURRENT).getModules();
      for (File module : modules) {
         File alternativeGradle = new File(module, GradleParseHelper.ALTERNATIVE_NAME);
         try (BufferedWriter writer = new BufferedWriter(new FileWriter(alternativeGradle))) {
            writer.write(ALTERNATIVE_FILE_CONTENT);
         }
      }
      
   }
   
   @Test
   public void testReplacement() throws IOException, XmlPullParserException, InterruptedException {
      prepareBuildfiles(true);
      
      List<File> modules = GradleParseUtil.getModules(TestBuildGradle.CURRENT).getModules();
      for (File module : modules) {
         final File gradleFile = GradleParseHelper.findGradleFile(module);
         String fileContent = FileUtils.readFileToString(gradleFile, StandardCharsets.UTF_8);
         MatcherAssert.assertThat(fileContent, Matchers.containsString(ALTERNATIVE_FILE_CONTENT));
      }
   }
   
   @Test
   public void testNoReplacement() throws IOException, XmlPullParserException, InterruptedException {
      prepareBuildfiles(false);
      
      List<File> modules = GradleParseUtil.getModules(TestBuildGradle.CURRENT).getModules();
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
      
      PeassFolders folders = new PeassFolders(TestBuildGradle.CURRENT);
      Mockito.when(mockedTransformer.getProjectFolder()).thenReturn(TestBuildGradle.CURRENT);
      
      GradleTestExecutor gte = new GradleTestExecutor(folders, mockedTransformer, new EnvironmentVariables());
      
      gte.prepareKoPeMeExecution(new File(TestBuildGradle.CURRENT, "out.txt"));
   }

   
}
