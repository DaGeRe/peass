package de.dagere.peass.transformation;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;

public class TestIgnoredClassTransformation {

   private static final File RESOURCE_FOLDER = new File("src/test/resources/transformation/ignored");

   @TempDir
   public static File testFolder;


   @BeforeEach
   public void initFolder() throws URISyntaxException, IOException {
      FileUtils.copyFile(new File(RESOURCE_FOLDER, "../pom.xml"), new File(testFolder, "pom.xml"));
   }

   @Test
   public void testJUnit4Ignore() throws IOException {
      TestSet tests = TestIgnoredMethodTransformation.executeTransformation("TestClassIgnored.java", testFolder);
      
      MatcherAssert.assertThat(tests.getTests(), Matchers.not(IsIterableContaining.hasItem(new TestCase("TestClassIgnored#testMe"))));
   }
   
   @Test
   public void testJUnit5Disabled() throws IOException {
      TestSet tests = TestIgnoredMethodTransformation.executeTransformation("TestClassDisabled.java", testFolder);
      
      MatcherAssert.assertThat(tests.getTests(), Matchers.not(IsIterableContaining.hasItem(new TestCase("TestClassDisabled#testMe"))));
   }
   
   @Test
   public void testJUnit5KoPeMeIgnore() throws IOException {
      TestSet tests = TestIgnoredMethodTransformation.executeTransformation("TestClassKoPeMeIgnore.java", testFolder);
      
      MatcherAssert.assertThat(tests.getTests(), Matchers.not(IsIterableContaining.hasItem(new TestCase("TestClassKoPeMeIgnore#testMe"))));
   }
}
