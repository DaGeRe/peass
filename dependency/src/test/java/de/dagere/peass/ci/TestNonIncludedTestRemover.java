package de.dagere.peass.ci;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.ci.NonIncludedTestRemover;
import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.analysis.data.TestSet;

public class TestNonIncludedTestRemover {

   @Test
   public void testWithSet() {
      Set<TestCase> tests = new HashSet<TestCase>();
      tests.add(new TestCase("TestA", "method1"));
      tests.add(new TestCase("TestA", "method2"));
      tests.add(new TestCase("TestB", "method3"));
      tests.add(new TestCase("TestC", null));
      tests.add(new TestCase("TestD", null));

      NonIncludedTestRemover.removeNotIncluded(tests, new ExecutionConfig(Arrays.asList(new String[] { "TestA#method1", "TestC" }), "test"));

      Assert.assertEquals(2, tests.size());
   }

   @Test
   public void testWithTestSet() {
      TestSet tests = new TestSet();
      tests.addTest(new TestCase("TestA", "method1"));
      tests.addTest(new TestCase("TestA", "method2"));
      tests.addTest(new TestCase("TestB", "method3"));
      tests.addTest(new TestCase("TestC", null));
      tests.addTest(new TestCase("TestD", null));

      NonIncludedTestRemover.removeNotIncluded(tests, new ExecutionConfig(Arrays.asList(new String[] { "TestA#method1", "TestC" }), "test"));

      Assert.assertEquals(2, tests.getTests().size());
   }
}
