package de.dagere.peass.transformation;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class TestIgnoredClassTransformation {

   private static final File RESOURCE_FOLDER = new File("src/test/resources/transformation/ignored");

   @TempDir
   public static File testFolder;

   private File sourcesFolder;
   private File testFile;

   @BeforeEach
   public void initFolder() throws URISyntaxException, IOException {
      sourcesFolder = new File(testFolder, "src/test/java");
      FileUtils.copyFile(new File(RESOURCE_FOLDER, "../pom.xml"), new File(testFolder, "pom.xml"));
   }

   @Test
   public void testJUnit4Ignore() throws IOException {
      final File old2 = new File(RESOURCE_FOLDER, "TestClassIgnored.java");
      testFile = new File(sourcesFolder, "TestClassIgnored.java");
      FileUtils.copyFile(old2, testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder, MeasurementConfig.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] { testFolder }));
      
      ProjectModules modules = new ProjectModules(testFolder);
      TestSet tests = tt.findModuleTests(ModuleClassMapping.SINGLE_MODULE_MAPPING, Arrays.asList(new String[]{""}), modules);
      
      MatcherAssert.assertThat(tests.getTests(), Matchers.not(IsIterableContaining.hasItem(new TestCase("TestClassIgnored#testMe"))));
   }
   
   @Test
   public void testJUnit5Disabled() throws IOException {
      final File old2 = new File(RESOURCE_FOLDER, "TestClassDisabled.java");
      testFile = new File(sourcesFolder, "TestClassDisabled.java");
      FileUtils.copyFile(old2, testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder, MeasurementConfig.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] { testFolder }));
      
      ProjectModules modules = new ProjectModules(testFolder);
      TestSet tests = tt.findModuleTests(ModuleClassMapping.SINGLE_MODULE_MAPPING, Arrays.asList(new String[]{""}), modules);
      
      MatcherAssert.assertThat(tests.getTests(), Matchers.not(IsIterableContaining.hasItem(new TestCase("TestClassDisabled#testMe"))));
   }
   
   @Test
   public void testJUnit5KoPeMeIgnore() throws IOException {
      final File old2 = new File(RESOURCE_FOLDER, "TestClassKoPeMeIgnore.java");
      testFile = new File(sourcesFolder, "TestClassKoPeMeIgnore.java");
      FileUtils.copyFile(old2, testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder, MeasurementConfig.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] { testFolder }));
      
      ProjectModules modules = new ProjectModules(testFolder);
      TestSet tests = tt.findModuleTests(ModuleClassMapping.SINGLE_MODULE_MAPPING, Arrays.asList(new String[]{""}), modules);
      
      MatcherAssert.assertThat(tests.getTests(), Matchers.not(IsIterableContaining.hasItem(new TestCase("TestClassKoPeMeIgnore#testMe"))));
   }
}
