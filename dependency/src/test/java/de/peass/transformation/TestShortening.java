package de.peass.transformation;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.testtransformation.JUnitTestShortener;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class TestShortening {

   private URL url = Thread.currentThread().getContextClassLoader().getResource("shortening/ExampleTest.java");
   private File exampleTestFile = new File(url.getPath());
   private URL subUrl = Thread.currentThread().getContextClassLoader().getResource("shortening/SubTest.java");
   private File subTestFile = new File(subUrl.getPath());

   JUnitTestTransformer transformer;

   @Rule
   public TemporaryFolder folder = new TemporaryFolder();

   @Before
   public void initFile() throws IOException {
      final File test = new File(folder.getRoot(), "src/test/java");
      test.mkdirs();
      final File testClazz = new File(test, "ExampleTest.java");
      FileUtils.copyFile(exampleTestFile, testClazz);
      final File subTestClazz = new File(test, "SubTest.java");
      FileUtils.copyFile(subTestFile, subTestClazz);

      transformer = new JUnitTestTransformer(folder.getRoot(), new MeasurementConfiguration(5));
      transformer.determineVersions(Arrays.asList(new File[] { folder.getRoot() }));
   }

   @Test
   public void testShortening() throws Exception {
      final File test = new File(folder.getRoot(), "src/test/java");
      test.mkdirs();
      final File testClazz = new File(test, "ExampleTest.java");

      try (JUnitTestShortener shortener = new JUnitTestShortener(transformer, folder.getRoot(), new ChangedEntity("ExampleTest", ""), "test1")) {
         Assert.assertFalse(FileUtils.contentEquals(exampleTestFile, testClazz));
      }

      Assert.assertTrue(FileUtils.contentEquals(exampleTestFile, testClazz));
   }

   @Test
   public void testSubclassShortening() throws Exception {
      final File test = new File(folder.getRoot(), "src/test/java");
      test.mkdirs();
      final File testClazz = new File(test, "ExampleTest.java");
      final File subClazz = new File(test, "SubTest.java");

      try (JUnitTestShortener shortener = new JUnitTestShortener(transformer, folder.getRoot(), new ChangedEntity("SubTest", ""), "test3")) {
         Assert.assertFalse(FileUtils.contentEquals(exampleTestFile, testClazz));
         Assert.assertFalse(FileUtils.contentEquals(subTestFile, subClazz));
      }

      Assert.assertTrue(FileUtils.contentEquals(exampleTestFile, testClazz));
      Assert.assertTrue(FileUtils.contentEquals(subTestFile, subClazz));
   }
}
