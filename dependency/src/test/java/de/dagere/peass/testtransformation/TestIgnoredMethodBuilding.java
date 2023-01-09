package de.dagere.peass.testtransformation;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.RunnableTestInformation;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class TestIgnoredMethodBuilding {

   private static final File RESOURCE_FOLDER = new File("src/test/resources/transformation/ignored");

   @TempDir
   public static File testFolder;

   @Test
   public void testJUnit4Ignore() throws IOException {
      RunnableTestInformation tests = executeTransformation("TestMeIgnored.java", testFolder);

      MatcherAssert.assertThat(tests.getTestsToUpdate().getTestMethods(), Matchers.not(IsIterableContaining.hasItem(new TestMethodCall("TestMeIgnored", "testMe1"))));
      MatcherAssert.assertThat(tests.getIgnoredTests().getTestMethods(), IsIterableContaining.hasItem(new TestMethodCall("TestMeIgnored", "testMe1")));

      MatcherAssert.assertThat(tests.getIgnoredTests().getTestMethods(), Matchers.not(IsIterableContaining.hasItem(new TestMethodCall("TestMeIgnored", "testMe2"))));
      MatcherAssert.assertThat(tests.getTestsToUpdate().getTestMethods(), IsIterableContaining.hasItem(new TestMethodCall("TestMeIgnored", "testMe2")));
   }

   @Test
   public void testJUnit4IgnoreClass() throws IOException {
      RunnableTestInformation tests = executeTransformation("TestClassIgnored.java", testFolder);

      MatcherAssert.assertThat(tests.getTestsToUpdate().getTestMethods(), Matchers.not(IsIterableContaining.hasItem(new TestMethodCall("TestClassIgnored", "testMe"))));
      MatcherAssert.assertThat(tests.getIgnoredTests().getTestMethods(), IsIterableContaining.hasItem(new TestMethodCall("TestClassIgnored", "testMe")));
   }
   
   public static RunnableTestInformation executeTransformation(final String currentClassName, final File testFolder) throws IOException {
      File sourcesFolder = initializeProject(testFolder);

      final File old2 = new File(RESOURCE_FOLDER, currentClassName);
      File testFile = new File(sourcesFolder, currentClassName);
      FileUtils.copyFile(old2, testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder, MeasurementConfig.DEFAULT);
      
      ModuleClassMapping mapping = Mockito.mock(ModuleClassMapping.class);
      Mockito.when(mapping.getModules()).thenReturn(Arrays.asList(new File[] { testFolder }));
      
      tt.determineVersions(mapping.getModules());
      
      RunnableTestInformation tests = tt.buildTestMethodSet(new TestSet(currentClassName.split("\\.")[0]), mapping);

      return tests;
   }

   private static File initializeProject(final File testFolder) throws IOException {
      File sourcesFolder = new File(testFolder, "src/test/java");
      FileUtils.copyFile(new File(RESOURCE_FOLDER, "../pom.xml"), new File(testFolder, "pom.xml"));
      return sourcesFolder;
   }
}
