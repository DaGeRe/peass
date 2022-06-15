package de.dagere.peass.ci;

import java.io.File;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class TestNonIncludedByRule {

   @Test
   public void testIncludeOneClass() {
      TestCase testWithoutRule = new TestCase("mypackage.MyTestWithoutRule", "testMe");
      TestCase testWithRule = new TestCase("mypackage.MyTestWithRule", "testMe");

      MeasurementConfig measurementConfig = new MeasurementConfig(2);
      measurementConfig.getExecutionConfig().setTestClazzFolders(Arrays.asList(new String[] { "" }));
      measurementConfig.getExecutionConfig().getIncludeByRule().add("DockerRule");
      JUnitTestTransformer transformer = new JUnitTestTransformer(new File("src/test/resources/includeByRuleExample/basic"), measurementConfig);
      transformer.determineVersions(Arrays.asList(new File[] {transformer.getProjectFolder()}));
      
      Assert.assertFalse(NonIncludedByRule.isTestIncluded(testWithoutRule, transformer));
      Assert.assertTrue(NonIncludedByRule.isTestIncluded(testWithRule, transformer));
   }
   
   @Test
   public void testExcludeOneClass() {
      TestCase testWithoutRule = new TestCase("mypackage.MyTestWithoutRule", "testMe");
      TestCase testWithRule = new TestCase("mypackage.MyTestWithRule", "testMe");

      MeasurementConfig measurementConfig = new MeasurementConfig(2);
      measurementConfig.getExecutionConfig().setTestClazzFolders(Arrays.asList(new String[] { "" }));
      measurementConfig.getExecutionConfig().getExcludeByRule().add("DockerRule");
      JUnitTestTransformer transformer = new JUnitTestTransformer(new File("src/test/resources/includeByRuleExample/basic"), measurementConfig);
      transformer.determineVersions(Arrays.asList(new File[] {transformer.getProjectFolder()}));
      
      Assert.assertTrue(NonIncludedByRule.isTestIncluded(testWithoutRule, transformer));
      Assert.assertFalse(NonIncludedByRule.isTestIncluded(testWithRule, transformer));
   }

   @Test
   public void testSuperClass() {

   }
}
