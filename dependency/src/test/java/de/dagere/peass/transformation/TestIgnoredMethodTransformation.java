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

public class TestIgnoredMethodTransformation {

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
      final File old2 = new File(RESOURCE_FOLDER, "TestMeIgnored.java");
      testFile = new File(sourcesFolder, "TestMeIgnored.java");
      FileUtils.copyFile(old2, testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder, MeasurementConfig.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] { testFolder }));
      
      ProjectModules modules = new ProjectModules(testFolder);
      TestSet tests = tt.findModuleTests(ModuleClassMapping.SINGLE_MODULE_MAPPING, Arrays.asList(new String[]{""}), modules);
      
      MatcherAssert.assertThat(tests.getTests(), Matchers.not(IsIterableContaining.hasItem(new TestCase("TestMeIgnored#testMe1"))));
      MatcherAssert.assertThat(tests.getTests(), IsIterableContaining.hasItem(new TestCase("TestMeIgnored#testMe2")));
   }
   
   @Test
   public void testJUnit5Disabled() throws IOException {
      final File old2 = new File(RESOURCE_FOLDER, "TestMeDisabled.java");
      testFile = new File(sourcesFolder, "TestMeDisabled.java");
      FileUtils.copyFile(old2, testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder, MeasurementConfig.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] { testFolder }));
      
      ProjectModules modules = new ProjectModules(testFolder);
      TestSet tests = tt.findModuleTests(ModuleClassMapping.SINGLE_MODULE_MAPPING, Arrays.asList(new String[]{""}), modules);
      
      MatcherAssert.assertThat(tests.getTests(), Matchers.not(IsIterableContaining.hasItem(new TestCase("TestMeDisabled#testMe1"))));
      MatcherAssert.assertThat(tests.getTests(), IsIterableContaining.hasItem(new TestCase("TestMeDisabled#testMe2")));
   }
   
   @Test
   public void testJUnit5KoPeMeIgnore() throws IOException {
      final File old2 = new File(RESOURCE_FOLDER, "TestMeKoPeMeIgnore.java");
      testFile = new File(sourcesFolder, "TestMeKoPeMeIgnore.java");
      FileUtils.copyFile(old2, testFile);

      final JUnitTestTransformer tt = new JUnitTestTransformer(testFolder, MeasurementConfig.DEFAULT);
      tt.determineVersions(Arrays.asList(new File[] { testFolder }));
      
      ProjectModules modules = new ProjectModules(testFolder);
      TestSet tests = tt.findModuleTests(ModuleClassMapping.SINGLE_MODULE_MAPPING, Arrays.asList(new String[]{""}), modules);
      
      MatcherAssert.assertThat(tests.getTests(), Matchers.not(IsIterableContaining.hasItem(new TestCase("TestMeKoPeMeIgnore#testMe1"))));
      MatcherAssert.assertThat(tests.getTests(), IsIterableContaining.hasItem(new TestCase("TestMeKoPeMeIgnore#testMe2")));
   }
}
