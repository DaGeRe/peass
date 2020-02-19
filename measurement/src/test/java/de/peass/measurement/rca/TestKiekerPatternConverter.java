package de.peass.measurement.rca;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

import de.peass.measurement.rca.kieker.KiekerPatternConverter;

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

   @Test
   public void testFixParameters() throws Exception {
      String call = "public static long org.apache.commons.fileupload.util.Streams.copy(java.io.InputStream,java.io.OutputStream,boolean,byte[])";
      KiekerPatternConverter.fixParameters(call);
      KiekerPatternConverter.fixParameters("public int org.apache.commons.fileupload.MultipartStream$ItemInputStream.read(byte[], int, int)");

      KiekerPatternConverter.fixParameters("private boolean org.apache.commons.fileupload.FileUploadBase$FileItemIteratorImpl.findNextItem()");
      KiekerPatternConverter.fixParameters("public int org.apache.commons.fileupload.MultipartStream$ItemInputStream.read(byte[], int, int)");
      KiekerPatternConverter.fixParameters("public void org.apache.commons.fileupload.MultipartStream$ItemInputStream.close()");

   }
}
