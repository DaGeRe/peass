package de.dagere.peass.dependency.execution;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import de.dagere.peass.TestConstants;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class TestGradleParseUtil {

   @Test
   public void testModuleGetting() throws FileNotFoundException, IOException {
      List<File> modules = GradleParseUtil.getModules(new File("src/test/resources/gradle-multimodule-example")).getModules();

      MatcherAssert.assertThat(modules.size(), Matchers.is(3));
   }

   @Test
   public void testModuleGettingSpaces() throws FileNotFoundException, IOException {
      List<File> modules = GradleParseUtil.getModules(new File("src/test/resources/gradle-multimodule-example-spaces")).getModules();

      List<String> moduleNameList = modules.stream().map(file -> file.getName()).collect(Collectors.toList());
      MatcherAssert.assertThat(moduleNameList, IsIterableContaining.hasItem("gradle-multimodule-example-spaces"));
      MatcherAssert.assertThat(moduleNameList, IsIterableContaining.hasItem("myModule1"));
      MatcherAssert.assertThat(moduleNameList, IsIterableContaining.hasItem("myModule2"));
      MatcherAssert.assertThat(moduleNameList, IsIterableContaining.hasItem("myModule3"));
      MatcherAssert.assertThat(moduleNameList, IsIterableContaining.hasItem("myModule4"));
      

      MatcherAssert.assertThat(modules.size(), Matchers.is(5));
   }

   @Test
   public void testSubprojectInstrumentation() throws IOException, XmlPullParserException, InterruptedException {
      FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
      FileUtils.copyDirectory(new File("src/test/resources/gradle-multimodule-subprojectexample/"), TestConstants.CURRENT_FOLDER);

      PeassFolders folders = new PeassFolders(TestConstants.CURRENT_FOLDER);
      JUnitTestTransformer transformerMock = Mockito.mock(JUnitTestTransformer.class);
      MeasurementConfig measurementConfig = new MeasurementConfig(2);
      measurementConfig.setUseKieker(true);
      measurementConfig.getKiekerConfig().setUseSourceInstrumentation(false);
      Mockito.when(transformerMock.getConfig()).thenReturn(measurementConfig);
      GradleTestExecutor gradleTestExecutor = new GradleTestExecutor(folders, transformerMock, new EnvironmentVariables());
      gradleTestExecutor.setIncludedMethods(new HashSet<String>());

      gradleTestExecutor.prepareKoPeMeExecution(new File(TestConstants.CURRENT_FOLDER, "log.txt"));

      checkModuleFile();

      checkParentFile();
   }

   private void checkParentFile() throws IOException {
      File resultParentFile = new File(TestConstants.CURRENT_FOLDER, "build.gradle");
      String gradleParentContent = FileUtils.readFileToString(resultParentFile, Charset.defaultCharset());
      MatcherAssert.assertThat(gradleParentContent, Matchers.containsString("kopeme"));
      String afterTest = gradleParentContent.substring(gradleParentContent.indexOf("test {"));

      MatcherAssert.assertThat(afterTest, Matchers.containsString("-javaagent"));
      System.out.println(afterTest);
   }

   private void checkModuleFile() throws IOException {
      File resultFile = new File(TestConstants.CURRENT_FOLDER, "myModule1" + File.separator + "gradle-multimodule-example.gradle");
      String gradleResultFileContent = FileUtils.readFileToString(resultFile, Charset.defaultCharset());
      MatcherAssert.assertThat(gradleResultFileContent, Matchers.containsString("kopeme"));
   }
}
