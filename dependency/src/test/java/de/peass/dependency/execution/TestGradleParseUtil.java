package de.peass.dependency.execution;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.hamcrest.Matchers;
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
      
      Assert.assertThat(modules.size(), Matchers.is(3));
   }
   
   @Test
   public void testSubprojectInstrumentation() throws IOException, XmlPullParserException, InterruptedException {
      FileUtils.deleteDirectory(TestConstants.CURRENT_FOLDER);
      FileUtils.copyDirectory(new File("src/test/resources/gradle-multimodule-subprojectexample/"), TestConstants.CURRENT_FOLDER);
      
      PeASSFolders folders = new PeASSFolders(TestConstants.CURRENT_FOLDER);
      JUnitTestTransformer transformerMock = Mockito.mock(JUnitTestTransformer.class);
      Mockito.when(transformerMock.getConfig()).thenReturn(new MeasurementConfiguration(2));
      GradleTestExecutor gradleTestExecutor = new GradleTestExecutor(folders, transformerMock, new EnvironmentVariables());
      
      gradleTestExecutor.prepareKoPeMeExecution(new File(TestConstants.CURRENT_FOLDER, "log.txt"));
      
      File resultFile = new File(TestConstants.CURRENT_FOLDER, "myModule1" + File.separator + "gradle-multimodule-example.gradle");
      
      String gradlefile = FileUtils.readFileToString(resultFile, Charset.defaultCharset());
      Assert.assertThat(gradlefile, Matchers.containsString("kopeme"));
   }
}
