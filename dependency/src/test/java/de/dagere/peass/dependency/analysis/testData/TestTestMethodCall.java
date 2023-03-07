package de.dagere.peass.dependency.analysis.testData;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestTestMethodCall {
   
   @Test
   public void testNormalModuleConstruction() {
      TestMethodCall call = TestMethodCall.createFromString("moduleA§de.dagere.peass.ClazzA#methodA");
      Assert.assertEquals("moduleA", call.getModule());
      Assert.assertEquals("methodA", call.getMethod());
      Assert.assertEquals("de.dagere.peass.ClazzA", call.getClazz());
   }
   
   @Test
   public void testFailure() {
      Assertions.assertThrows(RuntimeException.class, () -> {
         TestMethodCall call = TestMethodCall.createFromString("moduleA§de/dagere/peass/ClazzA#methodA");
      });
   }
   
   @Test
   public void testSubmoduleConstruction() {
      TestMethodCall call = TestMethodCall.createFromString("moduleA/moduleB§de.dagere.peass.ClazzA#methodA");
      Assert.assertEquals("moduleA/moduleB", call.getModule());
      Assert.assertEquals("methodA", call.getMethod());
      Assert.assertEquals("de.dagere.peass.ClazzA", call.getClazz());
   }
}
