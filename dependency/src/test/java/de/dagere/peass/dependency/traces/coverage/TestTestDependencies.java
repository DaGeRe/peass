package de.dagere.peass.dependency.traces.coverage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsIterableContaining;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.dependency.analysis.data.ChangeTestMapping;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.TestDependencies;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.dependency.changesreading.ClazzChangeData;

public class TestTestDependencies {
   
   private static final TestMethodCall testEntity = new TestMethodCall("package.ClazzA", "testA", "moduleA");
   
   @Test
   public void testBasicAdding() {
      TestDependencies dependencies = buildTestDependencies();
      
      Set<String> calledMethods = dependencies.getDependencyMap().get(testEntity).getCalledMethods().get(new ChangedEntity("package.ClazzB", "moduleA"));
      MatcherAssert.assertThat(calledMethods, IsIterableContaining.hasItem("methodC(int)"));
      MatcherAssert.assertThat(calledMethods, IsIterableContaining.hasItem("methodB"));
      
      testNonParameterChange(dependencies);
      
      testParameterChange(dependencies, "methodC(int)");
   }
   
   @Test
   public void testAddingFQNParameter() {
      TestDependencies dependencies = buildTestDependenciesFQN();
      
      Set<String> calledMethods = dependencies.getDependencyMap().get(testEntity).getCalledMethods().get(new ChangedEntity("package.ClazzB", "moduleA"));
      MatcherAssert.assertThat(calledMethods, IsIterableContaining.hasItem("methodC(java.lang.Integer)"));
      MatcherAssert.assertThat(calledMethods, IsIterableContaining.hasItem("methodB"));
      
      testNonParameterChange(dependencies);
      
      testParameterChange(dependencies, "methodC(java.lang.Integer)");
   }
   
   @Test
   public void testClassLevelChange() {
      TestDependencies dependencies = buildTestDependencies();
      
      HashMap<ChangedEntity, ClazzChangeData> changes = new HashMap<ChangedEntity, ClazzChangeData>();
      ClazzChangeData clazzChangeData = new ClazzChangeData(new ChangedEntity("package.ClazzA", "moduleA"), false);
      clazzChangeData.addChange("ClazzA", "methodB");
      changes.put(new ChangedEntity("package.ClazzA", "moduleA"), clazzChangeData);
      
      ChangeTestMapping changeTestMap = dependencies.getChangeTestMap(changes);
      
      Set<ChangedEntity> changedClazzes = changeTestMap.getChanges().keySet();
      MatcherAssert.assertThat(changedClazzes, IsIterableContaining.hasItem(new ChangedEntity("package.ClazzA", "moduleA", "methodB")));
      
      MatcherAssert.assertThat(changeTestMap.getChanges().get(new ChangedEntity("package.ClazzA", "moduleA", "methodB")), IsIterableContaining.hasItem(testEntity));
      MatcherAssert.assertThat(changeTestMap.getChanges().get(new ChangedEntity("package.ClazzA", "moduleA")), IsIterableContaining.hasItem(testEntity));
   }
   
   private TestDependencies buildTestDependenciesFQN() {
      TestDependencies dependencies = new TestDependencies();
      
      HashMap<ChangedEntity, Set<String>> calledClasses = new HashMap<>();
      Set<String> methods = new HashSet<>();
      methods.add("testA");
      calledClasses.put(new ChangedEntity("package.ClazzA", "moduleA"), methods);
      Set<String> methodsB = new HashSet<>();
      methodsB.add("methodB");
      methodsB.add("methodC(java.lang.Integer)");
      calledClasses.put(new ChangedEntity("package.ClazzB", "moduleA"), methodsB);
      
      dependencies.addDependencies(testEntity, calledClasses);
      return dependencies;
   }
   
   private TestDependencies buildTestDependencies() {
      TestDependencies dependencies = new TestDependencies();
      
      HashMap<ChangedEntity, Set<String>> calledClasses = new HashMap<>();
      Set<String> methods = new HashSet<>();
      methods.add("testA");
      calledClasses.put(new ChangedEntity("package.ClazzA", "moduleA"), methods);
      Set<String> methodsB = new HashSet<>();
      methodsB.add("methodB");
      methodsB.add("methodC(int)");
      calledClasses.put(new ChangedEntity("package.ClazzB", "moduleA"), methodsB);
      
      dependencies.addDependencies(testEntity, calledClasses);
      return dependencies;
   }

   private void testParameterChange(final TestDependencies dependencies, final String parameterizedMethod) {
      HashMap<ChangedEntity, ClazzChangeData> changeTestMap = new HashMap<ChangedEntity, ClazzChangeData>();
      ClazzChangeData classChangeData = new ClazzChangeData(new ChangedEntity("package.ClazzB", "moduleA"), true);
      classChangeData.addChange("ClazzB", parameterizedMethod);
      changeTestMap.put(new ChangedEntity("package.ClazzB", "moduleA"), classChangeData);
      ChangeTestMapping changes = dependencies.getChangeTestMap(changeTestMap);
      Assert.assertEquals(1, changes.getChanges().size());
   }

   private void testNonParameterChange(final TestDependencies dependencies) {
      HashMap<ChangedEntity, ClazzChangeData> changeTestMap = new HashMap<ChangedEntity, ClazzChangeData>();
      ClazzChangeData classChangeData = new ClazzChangeData(new ChangedEntity("package.ClazzB", "moduleA"), true);
      classChangeData.addChange("ClazzB", "methodB");
      changeTestMap.put(new ChangedEntity("package.ClazzB", "moduleA"), classChangeData);
      ChangeTestMapping changes = dependencies.getChangeTestMap(changeTestMap);
      Assert.assertEquals(1, changes.getChanges().size());
   }
}
