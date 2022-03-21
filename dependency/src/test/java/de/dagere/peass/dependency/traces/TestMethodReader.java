package de.dagere.peass.dependency.traces;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class TestMethodReader {
   
   @Test
   public void testSimple() {
      String result = MethodReader.getSimpleType("de.asdasd.asdsad.asd.MyType");
      Assert.assertEquals("MyType", result);
   }
   
   @Test
   public void testGenerics() {
      String result = MethodReader.getSimpleType("de.MyType<Generic>>");
      Assert.assertEquals("MyType", result);
   }
   
   @Test
   public void testGenericAndPackage() {
      String result = MethodReader.getSimpleType("de.MyType<Generic.A<A>>");
      Assert.assertEquals("MyType", result);
   }
}
