package de.dagere.peass.properties;

import java.io.File;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.measurement.rca.kieker.KiekerPatternConverter;


public class TestSourceKeys {
   
   @Test
   public void testParameterConversion() {
      String testPattern = "public de.peass.TestResult de.peass.mypackage.Clazz.doSomething(int,String,java.io.File)";
      String result = KiekerPatternConverter.getFileNameStart(testPattern);
      Assert.assertEquals(result, "de.peass.mypackage.Clazz" + File.separator + "doSomething_int_String_File");
   }
   
   @Test
   public void testKeyConversion() {
      String testPattern = "public de.peass.TestResult de.peass.mypackage.Clazz.doSomething(int,String,java.io.File)";
      String result = KiekerPatternConverter.getKey(testPattern);
      Assert.assertEquals(result, "de.peass.mypackage.Clazz.doSomething_int_String_File");
   }
}
