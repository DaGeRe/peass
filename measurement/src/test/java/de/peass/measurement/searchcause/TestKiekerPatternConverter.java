package de.peass.measurement.searchcause;

import org.junit.Assert;
import org.junit.Test;

import de.peass.measurement.searchcause.kieker.KiekerPatternConverter;

public class TestKiekerPatternConverter {

   @Test
   public void testStringConversion() {
      final String patternNormal = "public void ClassA.myTest()";
      final String withConstructor = "protected ClassA.<init>()";
      final String withConstructorArray = "protected ClassA.<init>(int[])";
      final String defaultVisibility = "ClassA.<init>(int[])";
      final String twoParameters = "protected java.util.List de.test.Test.testMe(byte[],java.lang.String)";

      Assert.assertEquals(patternNormal, KiekerPatternConverter.getKiekerPattern(patternNormal));
      Assert.assertEquals("protected new ClassA.<init>()", KiekerPatternConverter.getKiekerPattern(withConstructor));
      Assert.assertEquals("protected new ClassA.<init>(int[])", KiekerPatternConverter.getKiekerPattern(withConstructorArray));
      Assert.assertEquals("new ClassA.<init>(int[])", KiekerPatternConverter.getKiekerPattern(defaultVisibility));
      Assert.assertEquals(twoParameters, KiekerPatternConverter.getKiekerPattern(twoParameters));
   }
}
