package de.dagere.peass.dependency.traces.coverage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;

import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestDependencies;

public class TestTestDependencies {
   
   private static final ChangedEntity testEntity = new ChangedEntity("ClazzA", "moduleA", "testA");
   
   @Test
   public void testAdding() {
      TestDependencies dependencies = new TestDependencies();
      
      HashMap<ChangedEntity, Set<String>> calledClasses = new HashMap<>();
      Set<String> methods = new HashSet<>();
      methods.add("testA");
      calledClasses.put(new ChangedEntity("ClazzA", "moduleA"), methods);
      Set<String> methodsB = new HashSet<>();
      methodsB.add("methodB");
      methodsB.add("methodB(int)");
      calledClasses.put(new ChangedEntity("ClazzB", "moduleA"), methodsB);
      
      dependencies.addDependencies(testEntity, calledClasses);
      
      Set<String> calledMethods = dependencies.getDependencyMap().get(testEntity).getCalledMethods().get(new ChangedEntity("ClazzB", "moduleA"));
      MatcherAssert.assertThat(calledMethods, IsIterableContaining.hasItem("methodB(int)"));
      MatcherAssert.assertThat(calledMethods, IsIterableContaining.hasItem("methodB"));
   }
}
