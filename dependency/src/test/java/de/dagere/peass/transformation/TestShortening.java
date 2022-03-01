package de.dagere.peass.transformation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.testtransformation.JUnitTestShortener;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class TestShortening {

   private URL url = Thread.currentThread().getContextClassLoader().getResource("shortening/ExampleTest.java");
   private File exampleTestFile = new File(url.getPath());
   private URL url5 = Thread.currentThread().getContextClassLoader().getResource("shortening/ExampleTestJUnit5.java");
   private File exampleTestFile5 = new File(url5.getPath());
   private URL url5parameterized = Thread.currentThread().getContextClassLoader().getResource("shortening/ExampleTestJUnit5Parameterized.java");
   private File exampleTestFile5Parameterized = new File(url5parameterized.getPath());
   private URL subUrl = Thread.currentThread().getContextClassLoader().getResource("shortening/SubTest.java");
   private File subTestFile = new File(subUrl.getPath());

   JUnitTestTransformer transformer;

   @TempDir
   public File folder;

   @BeforeEach
   public void initFile() throws IOException {
      final File test = new File(folder, "src/test/java/de");
      test.mkdirs();
      final File testClazz = new File(test, "ExampleTest.java");
      FileUtils.copyFile(exampleTestFile, testClazz);
      final File testClazz5 = new File(test, "ExampleTestJUnit5.java");
      FileUtils.copyFile(exampleTestFile5, testClazz5);
      final File testClazz5Parameterized = new File(test, "ExampleTestJUnit5Parameterized.java");
      FileUtils.copyFile(exampleTestFile5Parameterized, testClazz5Parameterized);
      final File subTestClazz = new File(test, "SubTest.java");
      FileUtils.copyFile(subTestFile, subTestClazz);

      transformer = new JUnitTestTransformer(folder, new MeasurementConfig(5));
      transformer.determineVersions(Arrays.asList(new File[] { folder }));
   }

   @Test
   public void testShortening() throws Exception {
      final File test = new File(folder, "src/test/java/de");
      test.mkdirs();
      final File testClazz = new File(test, "ExampleTest.java");

      try (JUnitTestShortener shortener = new JUnitTestShortener(transformer, folder, new ChangedEntity("de.ExampleTest", ""), "test1")) {
         Assert.assertFalse(FileUtils.contentEquals(exampleTestFile, testClazz));
      }

      Assert.assertTrue(FileUtils.contentEquals(exampleTestFile, testClazz));
   }

   @Test
   public void testShorteningJUnit5() throws Exception {
      final File test = new File(folder, "src/test/java/de");
      test.mkdirs();
      final File testClazz = new File(test, "ExampleTestJUnit5.java");

      try (JUnitTestShortener shortener = new JUnitTestShortener(transformer, folder, new ChangedEntity("de.ExampleTestJUnit5", ""), "checkSomething")) {
         Assert.assertFalse(FileUtils.contentEquals(exampleTestFile5, testClazz));
         try (FileInputStream inputStream = new FileInputStream(testClazz)) {
            String fileContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            int matches = StringUtils.countMatches(fileContent, "@Test");
            Assert.assertEquals(1, matches);
         }
      }

      Assert.assertTrue(FileUtils.contentEquals(exampleTestFile5, testClazz));
   }

   @Test
   public void testShorteningJUnit5Parameterized() throws Exception {
      final File test = new File(folder, "src/test/java/de");
      test.mkdirs();
      final File testClazz = new File(test, "ExampleTestJUnit5Parameterized.java");

      try (JUnitTestShortener shortener = new JUnitTestShortener(transformer, folder, new ChangedEntity("de.ExampleTestJUnit5Parameterized", ""), "checkSomething")) {
         Assert.assertFalse(FileUtils.contentEquals(exampleTestFile5Parameterized, testClazz));
         try (FileInputStream inputStream = new FileInputStream(testClazz)) {
            String fileContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            System.out.println(fileContent);
            int matches = StringUtils.countMatches(fileContent, "@Test");
            Assert.assertEquals(1, matches);
            int matchesParameterized = StringUtils.countMatches(fileContent, "@ParameterizedTest");
            Assert.assertEquals(0, matchesParameterized);
         }
      }

      Assert.assertTrue(FileUtils.contentEquals(exampleTestFile5Parameterized, testClazz));
   }

   @Test
   public void testShorteningJUnit5Parameterized_methodItself() throws Exception {
      final File test = new File(folder, "src/test/java/de");
      test.mkdirs();
      final File testClazz = new File(test, "ExampleTestJUnit5Parameterized.java");

      try (JUnitTestShortener shortener = new JUnitTestShortener(transformer, folder, new ChangedEntity("de.ExampleTestJUnit5Parameterized", ""), "testMe")) {
         Assert.assertFalse(FileUtils.contentEquals(exampleTestFile5Parameterized, testClazz));
         try (FileInputStream inputStream = new FileInputStream(testClazz)) {
            String fileContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            System.out.println(fileContent);
            int matches = StringUtils.countMatches(fileContent, "@Test");
            Assert.assertEquals(0, matches);
            int matchesParameterized = StringUtils.countMatches(fileContent, "@ParameterizedTest");
            Assert.assertEquals(1, matchesParameterized);
         }
      }

      Assert.assertTrue(FileUtils.contentEquals(exampleTestFile5Parameterized, testClazz));
   }

   @Test
   public void testSubclassShortening() throws Exception {
      final File test = new File(folder, "src/test/java/de");
      test.mkdirs();
      final File testClazz = new File(test, "ExampleTest.java");
      final File subClazz = new File(test, "SubTest.java");

      try (JUnitTestShortener shortener = new JUnitTestShortener(transformer, folder, new ChangedEntity("de.SubTest", ""), "test3")) {
         Assert.assertFalse(FileUtils.contentEquals(exampleTestFile, testClazz));
         Assert.assertFalse(FileUtils.contentEquals(subTestFile, subClazz));
      }

      Assert.assertTrue(FileUtils.contentEquals(exampleTestFile, testClazz));
      Assert.assertTrue(FileUtils.contentEquals(subTestFile, subClazz));
   }
}
