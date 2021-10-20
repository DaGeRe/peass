package net.kieker.sourceinstrumentation;

import java.io.FileNotFoundException;
import java.util.HashSet;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import net.kieker.sourceinstrumentation.instrument.SignatureMatchChecker;

public class TestSignatureMatching {

   @Test
   public void testInclusionSignature() throws FileNotFoundException {
      HashSet<String> includes = new HashSet<String>();
      includes.add("public * de.peass.Example.methodA(int)");
      HashSet<String> excludes = new HashSet<String>();
      
      SignatureMatchChecker matchChecker = new SignatureMatchChecker(includes, excludes);
      Assert.assertTrue(matchChecker.testSignatureMatch("public void de.peass.Example.methodA(int)"));
      Assert.assertTrue(matchChecker.testSignatureMatch("public int de.peass.Example.methodA(int)"));
      Assert.assertFalse(matchChecker.testSignatureMatch("public void de.peass.Example.methodA(int, String)"));
      Assert.assertFalse(matchChecker.testSignatureMatch("protected void de.peass.Example.methodA(int)"));
   }
   
   @Test
   public void testExclusionSignature() {
      HashSet<String> includes = new HashSet<String>();
      includes.add("*");
      HashSet<String> excludes = new HashSet<String>();
      excludes.add("public * de.peass.Example.*(..)");
      excludes.add("public *.*.* de.peass.Example.*(..)");
      excludes.add("public *.* de.peass.Example.*(..)");
      excludes.add("public * de.peass.Example.*(..)");
      excludes.add("public * de.peass.Example.<init>(..)"); // <init> needs to be added separately
      
      SignatureMatchChecker matchChecker = new SignatureMatchChecker(includes, excludes);
      Assert.assertTrue(matchChecker.testSignatureMatch("public void de.peass.OtherClass.methodA(int)"));
      Assert.assertFalse(matchChecker.testSignatureMatch("public int de.peass.Example.methodA(int)"));
      Assert.assertFalse(matchChecker.testSignatureMatch("public void de.peass.Example.methodA(int, String)"));
      Assert.assertFalse(matchChecker.testSignatureMatch("public new de.peass.Example.<init>(int, String)"));
      Assert.assertFalse(matchChecker.testSignatureMatch("public de.ResultClass de.peass.Example.calculateStuff(int, String)"));
   }
}
