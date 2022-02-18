package de.dagere.peass.dependency.analysis.data;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;

public class TestTestSet {
   
   @Test
   public void testSimpleAdding() {
      TestSet tests = new TestSet();
      tests.addTest(new TestCase("ClazzA", "methodA", "moduleA", "var-5"));
      
      MatcherAssert.assertThat(tests.getTests(), IsIterableContaining.hasItem(new TestCase("ClazzA", "methodA", "moduleA", "var-5")));
   }
}
