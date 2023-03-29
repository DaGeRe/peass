package de.dagere.peass.analysis.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.nodeDiffGenerator.data.MethodCall;
import de.dagere.peass.config.ExecutionConfig;

public class TestMethodChangeLongMethodName {

   public static final File methodSourceFolder = new File("target" + File.separator + "current_sources");
   public static final String COMMIT = "000001";
   public static final ExecutionConfig TEST_CONFIG = new ExecutionConfig();

   public void method(final int a) {
      System.out.println("Contains System.out");

   }

   public void method(final String a, final String b, final String c, final String d, final String e, final String f, final String g, final String h, final String j,
         final String k) {
      System.err.println("Contains System.err");
   }
   
   public void method(final String a, final String b, final String c, final String d, final String e, final String f, final String g, final String h, final String j,
         final String k, final String l, final String m, final String n, final String o, final String p, final String q) {
      System.err.println("This is too long for regular serialization");
   }
   
   public void method(final String a, final String b, final String c, final String d, final String e, final String f, final String g, final String h, final String j,
         final String k, final String l, final String m, final String n, final String o, final String p, final String q, final int intA) {
      System.err.println("Other too long method");
   }

   @BeforeEach
   public void before() {
      if (!methodSourceFolder.exists()) {
         methodSourceFolder.mkdirs();
      }
   }

   @Test
   public void testSourceWriting() throws FileNotFoundException, IOException {
      MethodCall smallEntity = writeSmallMethod(new File("."), methodSourceFolder);
      MethodCall longEntity = writeLongMethod(new File("."), methodSourceFolder);
      MethodCall tooLongEntity = writeTooLongMethod(new File("."), methodSourceFolder);
      MethodCall otherTooLongEntity = writeOtherTooLongMethod(new File("."), methodSourceFolder);

      checkSmallMethod(methodSourceFolder, smallEntity);
      checkLongMethod(methodSourceFolder, longEntity);
      checkTooLongMethod(methodSourceFolder, tooLongEntity);
      checkOtherTooLongMethod(methodSourceFolder, otherTooLongEntity);
   }

   public static MethodCall writeSmallMethod(final File sourceFolder, final File methodSourceFolder) throws FileNotFoundException, IOException {
      MethodCall smallMethodEntity = new MethodCall("de.dagere.peass.analysis.properties.TestMethodChangeLongMethodName", "", "method");
      smallMethodEntity.createParameters("int");
      MethodChangeReader reader2 = new MethodChangeReader(methodSourceFolder, sourceFolder,
            new File("."), smallMethodEntity, COMMIT, TEST_CONFIG);
      reader2.readMethodChangeData();
      return smallMethodEntity;
   }

   public static MethodCall writeLongMethod(final File sourceFolder, final File methodSourceFolder) throws FileNotFoundException, IOException {
      MethodCall longMethodEntity = new MethodCall("de.dagere.peass.analysis.properties.TestMethodChangeLongMethodName", "", "method");
      longMethodEntity.createParameters("java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,"
            + "java.lang.String,java.lang.String,java.lang.String,java.lang.String");
      MethodChangeReader reader = new MethodChangeReader(methodSourceFolder, sourceFolder,
            new File("."), longMethodEntity, COMMIT, TEST_CONFIG);
      reader.readMethodChangeData();
      return longMethodEntity;
   }
   
   public static MethodCall writeTooLongMethod(final File sourceFolder, final File methodSourceFolder) throws FileNotFoundException, IOException {
      MethodCall longMethodEntity = new MethodCall("de.dagere.peass.analysis.properties.TestMethodChangeLongMethodName", "", "method");
      longMethodEntity.createParameters("java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,"
            + "java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String");
      MethodChangeReader reader = new MethodChangeReader(methodSourceFolder, sourceFolder,
            new File("."), longMethodEntity, COMMIT, TEST_CONFIG);
      reader.readMethodChangeData();
      return longMethodEntity;
   }
   
   public static MethodCall writeOtherTooLongMethod(final File sourceFolder, final File methodSourceFolder) throws FileNotFoundException, IOException {
      MethodCall longMethodEntity = new MethodCall("de.dagere.peass.analysis.properties.TestMethodChangeLongMethodName", "", "method");
      longMethodEntity.createParameters("java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,"
            + "java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,java.lang.String,"
            + "java.lang.String,java.lang.String,int");
      MethodChangeReader reader = new MethodChangeReader(methodSourceFolder, sourceFolder,
            new File("."), longMethodEntity, COMMIT, TEST_CONFIG);
      reader.readMethodChangeData();
      return longMethodEntity;
   }

   private void checkSmallMethod(final File methodSourceFolder, final MethodCall smallEntity) throws IOException {
      File expectedConstructorFile = new ChangedMethodManager(methodSourceFolder).getMethodDiffFile(COMMIT, smallEntity);
      String constructorContent = FileUtils.readFileToString(expectedConstructorFile, Charset.defaultCharset());
      MatcherAssert.assertThat(constructorContent, Matchers.containsString("System.out"));
      MatcherAssert.assertThat(constructorContent, Matchers.not(Matchers.containsString("System.err")));
   }

   private void checkLongMethod(final File methodSourceFolder, final MethodCall longEntity) throws IOException {
      File expectedInitFile = new ChangedMethodManager(methodSourceFolder).getMethodDiffFile(COMMIT, longEntity);
      String initContent = FileUtils.readFileToString(expectedInitFile, Charset.defaultCharset());
      MatcherAssert.assertThat(initContent, Matchers.not(Matchers.containsString("System.out")));
      MatcherAssert.assertThat(initContent, Matchers.containsString("System.err"));
   }
   
   private void checkTooLongMethod(final File methodSourceFolder, final MethodCall longEntity) throws IOException {
      ChangedMethodManager changedMethodManager = new ChangedMethodManager(methodSourceFolder);
      File expectedMethodDiffFile = changedMethodManager.getMethodDiffFile(COMMIT, longEntity);
      String initContent = FileUtils.readFileToString(expectedMethodDiffFile, Charset.defaultCharset());
      MatcherAssert.assertThat(initContent, Matchers.not(Matchers.containsString("System.out")));
      MatcherAssert.assertThat(initContent, Matchers.containsString("This is too long for regular serialization"));
      
      File expectedMethodMainFile = changedMethodManager.getMethodMainFile(COMMIT, longEntity);
      Assert.assertEquals("method_1_main.txt", expectedMethodMainFile.getName());
   }
   
   private void checkOtherTooLongMethod(final File methodSourceFolder, final MethodCall longEntity) throws IOException {
      File expectedInitFile = new ChangedMethodManager(methodSourceFolder).getMethodDiffFile(COMMIT, longEntity);
      String initContent = FileUtils.readFileToString(expectedInitFile, Charset.defaultCharset());
      MatcherAssert.assertThat(initContent, Matchers.not(Matchers.containsString("System.out")));
      MatcherAssert.assertThat(initContent, Matchers.containsString("Other too long method"));
   }
}
