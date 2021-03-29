package de.peass.dependency.execution;

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
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import de.peass.TestConstants;
import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.PeASSFolders;
import de.peass.testtransformation.JUnitTestTransformer;

public class TestGradleParseUtil {

   @Test
   public void testModuleGetting() throws FileNotFoundException, IOException {
      List<File> modules = GradleParseUtil.getModules(new File("src/test/resources/gradle-multimodule-example")).getModules();

      Assert.assertThat(modules.size(), Matchers.is(3));
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
      

      Assert.assertThat(modules.size(), Matchers.is(5));
   }

   @Test
   public void testSubprojectInstrumentation() throws IOException, XmlPullParserException, InterruptedException {
      FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
      FileUtils.copyDirectory(new File("src/test/resources/gradle-multimodule-subprojectexample/"), TestConstants.CURRENT_FOLDER);

      PeASSFolders folders = new PeASSFolders(TestConstants.CURRENT_FOLDER);
      JUnitTestTransformer transformerMock = Mockito.mock(JUnitTestTransformer.class);
      MeasurementConfiguration measurementConfig = new MeasurementConfiguration(2);
      measurementConfig.setUseKieker(true);
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
      Assert.assertThat(gradleParentContent, Matchers.containsString("kopeme"));
      String afterTest = gradleParentContent.substring(gradleParentContent.indexOf("test {"));

      Assert.assertThat(afterTest, Matchers.containsString("-javaagent"));
      System.out.println(afterTest);
   }

   private void checkModuleFile() throws IOException {
      File resultFile = new File(TestConstants.CURRENT_FOLDER, "myModule1" + File.separator + "gradle-multimodule-example.gradle");
      String gradleResultFileContent = FileUtils.readFileToString(resultFile, Charset.defaultCharset());
      Assert.assertThat(gradleResultFileContent, Matchers.containsString("kopeme"));
   }
}
