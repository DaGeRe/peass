package de.dagere.peass.dependency.traces.coverage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsIterableContaining;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.nodeDiffDetector.data.Type;
import de.dagere.nodeDiffDetector.diffDetection.ClazzChangeData;
import de.dagere.peass.dependency.analysis.data.ChangeTestMapping;
import de.dagere.peass.dependency.analysis.data.TestDependencies;

public class TestTestDependencies {
   
   private static final TestMethodCall testEntity = new TestMethodCall("package.ClazzA", "testA", "moduleA");
   
   @Test
   public void testBasicAdding() {
      TestDependencies dependencies = buildTestDependencies();
      
      Set<String> calledMethods = dependencies.getDependencyMap().get(testEntity).getCalledMethods().get(new Type("package.ClazzB", "moduleA"));
      MatcherAssert.assertThat(calledMethods, IsIterableContaining.hasItem("methodC(int)"));
      MatcherAssert.assertThat(calledMethods, IsIterableContaining.hasItem("methodB"));
      
      testNonParameterChange(dependencies);
      
      testParameterChange(dependencies, "methodC(int)");
   }
   
   @Test
   public void testAddingFQNParameter() {
      TestDependencies dependencies = buildTestDependenciesFQN();
      
      Set<String> calledMethods = dependencies.getDependencyMap().get(testEntity).getCalledMethods().get(new Type("package.ClazzB", "moduleA"));
      MatcherAssert.assertThat(calledMethods, IsIterableContaining.hasItem("methodC(java.lang.Integer)"));
      MatcherAssert.assertThat(calledMethods, IsIterableContaining.hasItem("methodB"));
      
      testNonParameterChange(dependencies);
      
      testParameterChange(dependencies, "methodC(java.lang.Integer)");
   }
   
   @Test
   public void testClassLevelChange() {
      TestDependencies dependencies = buildTestDependencies();
      
      HashMap<Type, ClazzChangeData> changes = new HashMap<Type, ClazzChangeData>();
      ClazzChangeData clazzChangeData = new ClazzChangeData(new Type("package.ClazzA", "moduleA"), false);
      clazzChangeData.addChange("ClazzA", "methodB");
      changes.put(new Type("package.ClazzA", "moduleA"), clazzChangeData);
      
      ChangeTestMapping changeTestMap = dependencies.getChangeTestMap(changes);
      
      Set<Type> changedClazzes = changeTestMap.getChanges().keySet();
      MatcherAssert.assertThat(changedClazzes, IsIterableContaining.hasItem(new MethodCall("package.ClazzA", "moduleA", "methodB")));
      
      MatcherAssert.assertThat(changeTestMap.getChanges().get(new MethodCall("package.ClazzA", "moduleA", "methodB")), IsIterableContaining.hasItem(testEntity));
      MatcherAssert.assertThat(changeTestMap.getChanges().get(new Type("package.ClazzA", "moduleA")), IsIterableContaining.hasItem(testEntity));
   }
   
   private TestDependencies buildTestDependenciesFQN() {
      TestDependencies dependencies = new TestDependencies();
      
      HashMap<Type, Set<String>> calledClasses = new HashMap<>();
      Set<String> methods = new HashSet<>();
      methods.add("testA");
      calledClasses.put(new Type("package.ClazzA", "moduleA"), methods);
      Set<String> methodsB = new HashSet<>();
      methodsB.add("methodB");
      methodsB.add("methodC(java.lang.Integer)");
      calledClasses.put(new Type("package.ClazzB", "moduleA"), methodsB);
      
      dependencies.addDependencies(testEntity, calledClasses);
      return dependencies;
   }
   
   private TestDependencies buildTestDependencies() {
      TestDependencies dependencies = new TestDependencies();
      
      HashMap<Type, Set<String>> calledClasses = new HashMap<>();
      Set<String> methods = new HashSet<>();
      methods.add("testA");
      calledClasses.put(new Type("package.ClazzA", "moduleA"), methods);
      Set<String> methodsB = new HashSet<>();
      methodsB.add("methodB");
      methodsB.add("methodC(int)");
      calledClasses.put(new Type("package.ClazzB", "moduleA"), methodsB);
      
      dependencies.addDependencies(testEntity, calledClasses);
      return dependencies;
   }

   private void testParameterChange(final TestDependencies dependencies, final String parameterizedMethod) {
      HashMap<Type, ClazzChangeData> changeTestMap = new HashMap<Type, ClazzChangeData>();
      ClazzChangeData classChangeData = new ClazzChangeData(new Type("package.ClazzB", "moduleA"), true);
      classChangeData.addChange("ClazzB", parameterizedMethod);
      changeTestMap.put(new Type("package.ClazzB", "moduleA"), classChangeData);
      ChangeTestMapping changes = dependencies.getChangeTestMap(changeTestMap);
      Assert.assertEquals(1, changes.getChanges().size());
   }

   private void testNonParameterChange(final TestDependencies dependencies) {
      HashMap<Type, ClazzChangeData> changeTestMap = new HashMap<Type, ClazzChangeData>();
      ClazzChangeData classChangeData = new ClazzChangeData(new Type("package.ClazzB", "moduleA"), true);
      classChangeData.addChange("ClazzB", "methodB");
      changeTestMap.put(new Type("package.ClazzB", "moduleA"), classChangeData);
      ChangeTestMapping changes = dependencies.getChangeTestMap(changeTestMap);
      Assert.assertEquals(1, changes.getChanges().size());
   }
}
