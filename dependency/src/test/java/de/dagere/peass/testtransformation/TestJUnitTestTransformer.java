package de.dagere.peass.testtransformation;

import java.io.File;
import java.util.Arrays;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;

import de.dagere.nodeDiffDetector.data.TestClazzCall;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.config.MeasurementConfig;

public class TestJUnitTestTransformer {

   @Test
   public void testTransformation() {
      File projectFolder = new File("src/test/resources/transformation/findStaticMethods");
      JUnitTestTransformer transformer = new JUnitTestTransformer(projectFolder, new MeasurementConfig(5));
      transformer.determineVersions(Arrays.asList(projectFolder));

      Set<TestMethodCall> testMethodNamesOuter = transformer.getTestMethodNames(projectFolder, new TestClazzCall("demo.project.gradle.ExampleTest"));
      System.out.println("Outer: " + testMethodNamesOuter);
      MatcherAssert.assertThat(testMethodNamesOuter, IsIterableContaining.hasItem(new TestMethodCall("demo.project.gradle.ExampleTest", "test", "")));

      Set<TestMethodCall> testMethodNamesInner = transformer.getTestMethodNames(projectFolder, new TestClazzCall("demo.project.gradle.ExampleTest$SenselessClazz"));
      System.out.println("Inner: " + testMethodNamesInner);
      MatcherAssert.assertThat(testMethodNamesInner, Matchers.not(IsIterableContaining.hasItem(new TestMethodCall("demo.project.gradle.ExampleTest$SenselessClazz", "test", ""))));
      
   }
}
