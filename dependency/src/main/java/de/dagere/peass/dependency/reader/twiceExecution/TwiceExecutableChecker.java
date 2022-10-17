package de.dagere.peass.dependency.reader.twiceExecution;

import java.util.Map;
import java.util.Set;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.execution.utils.TestExecutor;
import de.dagere.peass.testtransformation.TestTransformer;

public class TwiceExecutableChecker {
   private final TestExecutor executor;
   private final TestTransformer transformer;

   public TwiceExecutableChecker(TestExecutor executor, TestTransformer transformer) {
      this.executor = executor;
      this.transformer = transformer;
   }

   public void checkTwiceExecution(Set<TestMethodCall> tests) {
      
   }

   public Map<TestMethodCall, Boolean> getTestProperties() {
      // TODO Auto-generated method stub
      return null;
   }
}
