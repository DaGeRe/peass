package de.dagere.peass.dependency.analysis.data;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;

import de.dagere.nodeDiffDetector.data.TestMethodCall;

public class TestTestSet {
   
   @Test
   public void testSimpleAdding() {
      TestSet tests = new TestSet();
      tests.addTest(new TestMethodCall("ClazzA", "methodA", "moduleA", "var-5"));
      
      MatcherAssert.assertThat(tests.getTestMethods(), IsIterableContaining.hasItem(new TestMethodCall("ClazzA", "methodA", "moduleA", "var-5")));
   }
}
