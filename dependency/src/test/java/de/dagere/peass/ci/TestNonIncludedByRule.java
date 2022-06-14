package de.dagere.peass.ci;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;

public class TestNonIncludedByRule {

   @Disabled
   @Test
   public void testOneClass() {
      TestCase testWithoutRule = new TestCase("mypackage.MyTestWithoutRule", "testMe");
      TestCase testWithRule = new TestCase("mypackage.MyTestWithoutRule", "testMe");

      ExecutionConfig config = new ExecutionConfig();
      config.getIncludeByRule().add("DockerRule");

      config.setTestClazzFolders(Arrays.asList(new String[] { "src/test/resources/includeByRuleExample/basic" }));

      Assert.assertTrue(NonIncludedByRule.isTestIncluded(testWithoutRule, config));
      Assert.assertFalse(NonIncludedByRule.isTestIncluded(testWithRule, config));
   }

   @Test
   public void testSuperClass() {

   }
}
