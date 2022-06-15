package de.dagere.peass.ci;

import java.io.File;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class TestNonIncludedByRule {

   private static final File BASIC_EXAMPLE_FOLDER = new File("src/test/resources/includeByRuleExample/basic");
   private static final File SUPER_CLASS_EXAMPLE = new File("src/test/resources/includeByRuleExample/superClass");
   private static final File SUPER_SUPER_CLASS_EXAMPLE = new File("src/test/resources/includeByRuleExample/superSuperClass");
   
   private static final TestCase TEST_WITHOUT_RULE = new TestCase("mypackage.MyTestWithoutRule", "testMe");
   private static final TestCase TEST_WITH_RULE = new TestCase("mypackage.MyTestWithRule", "testMe");
   
   private static final TestCase SUB_TEST_WITHOUT_RULE = new TestCase("mypackage.SubTestWithoutRule", "testMe");
   private static final TestCase SUB_TEST_OF_TEST_WITH_RULE = new TestCase("mypackage.SubTestOfTestWithRule", "testMe");

   @Test
   public void testIncludeOneClass() {
      JUnitTestTransformer transformer = determineWithInclude(BASIC_EXAMPLE_FOLDER);
      
      Assert.assertFalse(NonIncludedByRule.isTestIncluded(TEST_WITHOUT_RULE, transformer));
      Assert.assertTrue(NonIncludedByRule.isTestIncluded(TEST_WITH_RULE, transformer));
   }

   @Test
   public void testExcludeOneClass() {
      JUnitTestTransformer transformer = determineWithExclude(BASIC_EXAMPLE_FOLDER);
      
      Assert.assertTrue(NonIncludedByRule.isTestIncluded(TEST_WITHOUT_RULE, transformer));
      Assert.assertFalse(NonIncludedByRule.isTestIncluded(TEST_WITH_RULE, transformer));
   }

   @Test
   public void testIncludeSuperClass() {
      JUnitTestTransformer transformer = determineWithInclude(SUPER_CLASS_EXAMPLE);
      
      Assert.assertFalse(NonIncludedByRule.isTestIncluded(TEST_WITHOUT_RULE, transformer));
      Assert.assertTrue(NonIncludedByRule.isTestIncluded(TEST_WITH_RULE, transformer));
      Assert.assertFalse(NonIncludedByRule.isTestIncluded(SUB_TEST_WITHOUT_RULE, transformer));
      Assert.assertTrue(NonIncludedByRule.isTestIncluded(SUB_TEST_OF_TEST_WITH_RULE, transformer));
   }
   
   @Test
   public void testExcludeSuperClass() {
      JUnitTestTransformer transformer = determineWithExclude(SUPER_CLASS_EXAMPLE);
      
      Assert.assertTrue(NonIncludedByRule.isTestIncluded(TEST_WITHOUT_RULE, transformer));
      Assert.assertFalse(NonIncludedByRule.isTestIncluded(TEST_WITH_RULE, transformer));
      Assert.assertTrue(NonIncludedByRule.isTestIncluded(SUB_TEST_WITHOUT_RULE, transformer));
      Assert.assertFalse(NonIncludedByRule.isTestIncluded(SUB_TEST_OF_TEST_WITH_RULE, transformer));
   }
   
   private JUnitTestTransformer determineWithInclude(File basicExampleFolder) {
      MeasurementConfig measurementConfig = new MeasurementConfig(2);
      measurementConfig.getExecutionConfig().setTestClazzFolders(Arrays.asList(new String[] { "" }));
      measurementConfig.getExecutionConfig().getIncludeByRule().add("DockerRule");
      JUnitTestTransformer transformer = new JUnitTestTransformer(basicExampleFolder, measurementConfig);
      transformer.determineVersions(Arrays.asList(new File[] {transformer.getProjectFolder()}));
      return transformer;
   }
   
   private JUnitTestTransformer determineWithExclude(File basicExampleFolder) {
      MeasurementConfig measurementConfig = new MeasurementConfig(2);
      measurementConfig.getExecutionConfig().setTestClazzFolders(Arrays.asList(new String[] { "" }));
      measurementConfig.getExecutionConfig().getExcludeByRule().add("DockerRule");
      
      JUnitTestTransformer transformer = new JUnitTestTransformer(basicExampleFolder, measurementConfig);
      transformer.determineVersions(Arrays.asList(new File[] {transformer.getProjectFolder()}));
      return transformer;
   }
}
