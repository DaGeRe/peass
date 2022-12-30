package de.dagere.peass.dependency;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.execution.gradle.AnboxTestExecutor;

public class TestAnboxTestExecutor {
   
   @Test
   public void testGetTestPackageName(){
      TestMethodCall test;
      
      test = new TestMethodCall("my.package.Clazz", "method");
      Assert.assertEquals("my.package.test", AnboxTestExecutor.getTestPackageName(test));

      test = new TestMethodCall("my.package.test.Clazz", "method");
      Assert.assertEquals("my.package.test", AnboxTestExecutor.getTestPackageName(test));
   }
}
