package de.dagere.peass.ci;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;
import de.dagere.peass.dependency.analysis.testData.TestClazzCall;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;

public class TestNonIncludedTestRemover {

   @Test
   public void testWithSet() {
      Set<TestCase> tests = new HashSet<TestCase>();
      tests.add(new TestMethodCall("TestA", "method1"));
      tests.add(new TestMethodCall("TestA", "method2"));
      tests.add(new TestMethodCall("TestB", "method3"));
      tests.add(new TestClazzCall("TestC"));
      tests.add(new TestClazzCall("TestD"));

      NonIncludedTestRemover.removeNotIncluded(tests, new ExecutionConfig(Arrays.asList(new String[] { "TestA#method1", "TestC" }), "test"));

      Assert.assertEquals(2, tests.size());
   }

   @Test
   public void testWithTestSet() {
      TestSet tests = new TestSet();
      tests.addTest(new TestMethodCall("TestA", "method1"));
      tests.addTest(new TestMethodCall("TestA", "method2"));
      tests.addTest(new TestMethodCall("TestB", "method3"));
      tests.addTest(new TestClazzCall("TestC"));
      tests.addTest(new TestClazzCall("TestD"));

      NonIncludedTestRemover.removeNotIncluded(tests, new ExecutionConfig(Arrays.asList(new String[] { "TestA#method1", "TestC" }), "test"));

      Assert.assertEquals(2, tests.getTests().size());
   }

   @Test
   public void testWithModulesRegular() {
      Set<TestCase> tests = new HashSet<TestCase>();
      tests.add(new TestMethodCall("TestA", "method1", "moduleA"));
      tests.add(new TestMethodCall("TestB", "method1", "moduleB"));
      tests.add(new TestMethodCall("TestC", "method1", "moduleC"));

      NonIncludedTestRemover.removeNotIncluded(tests, new ExecutionConfig(Arrays.asList(new String[] { "moduleA§TestA#method1", "TestC" }), "test"));

      System.out.println(tests);
      Assert.assertEquals(1, tests.size());
   }

   @Test
   public void testWithModulesSameNameButDifferentModule() {
      Set<TestCase> tests = new HashSet<TestCase>();
      tests.add(new TestMethodCall("TestA", "method1", "moduleA"));
      tests.add(new TestMethodCall("TestB", "method1", "moduleB"));
      tests.add(new TestMethodCall("TestA", "method1", "moduleC"));

      NonIncludedTestRemover.removeNotIncluded(tests, new ExecutionConfig(Arrays.asList(new String[] { "moduleA§TestA#method1", "TestC" }), "test"));

      System.out.println(tests);
      Assert.assertEquals(1, tests.size());
   }

   @Test
   public void testWithSetExclude() {
      Set<TestCase> tests = new HashSet<TestCase>();
      tests.add(new TestMethodCall("TestA", "method1"));
      tests.add(new TestMethodCall("TestA", "method2"));
      tests.add(new TestMethodCall("TestB", "method3"));
      tests.add(new TestClazzCall("TestC"));
      tests.add(new TestClazzCall("TestD"));

      ExecutionConfig executionConfig = new ExecutionConfig(Arrays.asList(new String[] { "TestA#*", "TestC" }), "test");
      executionConfig.setExcludes(Arrays.asList(new String[] { "TestA#method2" }));

      NonIncludedTestRemover.removeNotIncluded(tests, executionConfig);

      Assert.assertEquals(2, tests.size());
   }
   
   @Test
   public void testWithSetOnlyExclude() {
      Set<TestCase> tests = new HashSet<TestCase>();
      tests.add(new TestMethodCall("TestA", "method1"));
      tests.add(new TestMethodCall("TestA", "method2"));
      tests.add(new TestMethodCall("TestB", "method3"));
      tests.add(new TestClazzCall("TestC"));
      tests.add(new TestClazzCall("TestD"));

      ExecutionConfig executionConfig = new ExecutionConfig(Arrays.asList(new String[] { }), "test");
      executionConfig.setExcludes(Arrays.asList(new String[] { "TestA#*" }));

      NonIncludedTestRemover.removeNotIncluded(tests, executionConfig);

      Assert.assertEquals(3, tests.size());
   }
}
