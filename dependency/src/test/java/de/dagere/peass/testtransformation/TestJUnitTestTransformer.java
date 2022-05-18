package de.dagere.peass.testtransformation;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class TestJUnitTestTransformer {

   @Test
   public void testTransformation() {
      File projectFolder = new File("src/test/resources/transformation/findStaticMethods");
      JUnitTestTransformer transformer = new JUnitTestTransformer(projectFolder, new MeasurementConfig(5));
      transformer.determineVersions(Arrays.asList(projectFolder));

      List<TestCase> testMethodNamesOuter = transformer.getTestMethodNames(projectFolder, new TestCase("demo.project.gradle.ExampleTest"));
      System.out.println("Outer: " + testMethodNamesOuter);
      MatcherAssert.assertThat(testMethodNamesOuter, IsIterableContaining.hasItem(new TestCase("demo.project.gradle.ExampleTest#test")));

      List<TestCase> testMethodNamesInner = transformer.getTestMethodNames(projectFolder, new TestCase("demo.project.gradle.ExampleTest$SenselessClazz"));
      System.out.println("Inner: " + testMethodNamesInner);
      MatcherAssert.assertThat(testMethodNamesInner, Matchers.not(IsIterableContaining.hasItem(new TestCase("demo.project.gradle.ExampleTest$SenselessClazz#test"))));
      
   }
}
