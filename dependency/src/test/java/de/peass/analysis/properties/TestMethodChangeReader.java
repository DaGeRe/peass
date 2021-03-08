package de.peass.analysis.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.peass.dependency.analysis.data.ChangedEntity;

public class TestMethodChangeReader {
   
   public static final File methodSourceFolder = new File("target" + File.separator + "current_sources");

   private static final String VERSION = "000001";

   public TestMethodChangeReader() {
      System.out.println("Contains System.out");
   }

   public void init() {
      System.err.println("Contains System.err");
   }
   
   @Before
   public void before() {
      if (!methodSourceFolder.exists()) {
         methodSourceFolder.mkdirs();
      }
   }

   @Test
   public void testSourceWriting() throws FileNotFoundException, IOException {
      ChangedEntity constructorEntity = writeConstructor(methodSourceFolder);
      ChangedEntity initMethodEntity = writeInit(methodSourceFolder);
      
      checkConstructor(methodSourceFolder, constructorEntity);
      checkInitMethod(methodSourceFolder, initMethodEntity);
   }

   public static ChangedEntity writeInit(final File methodSourceFolder) throws FileNotFoundException, IOException {
      ChangedEntity initMethodEntity = new ChangedEntity("de.peass.analysis.properties.TestMethodChangeReader", "", "init");
      MethodChangeReader reader2 = new MethodChangeReader(methodSourceFolder, new File("."), 
            new File("."), initMethodEntity, VERSION);
      reader2.readMethodChangeData();
      return initMethodEntity;
   }

   public static ChangedEntity writeConstructor(final File methodSourceFolder) throws FileNotFoundException, IOException {
      ChangedEntity constructorEntity = new ChangedEntity("de.peass.analysis.properties.TestMethodChangeReader", "", "<init>");
      MethodChangeReader reader = new MethodChangeReader(methodSourceFolder, new File("."), 
            new File("."), constructorEntity, VERSION);
      reader.readMethodChangeData();
      return constructorEntity;
   }

   private void checkConstructor(final File methodSourceFolder, final ChangedEntity constructorEntity) throws IOException {
      File expectedConstructorFile = MethodChangeReader.getMethodDiffFile(methodSourceFolder, VERSION, constructorEntity);
      String constructorContent = FileUtils.readFileToString(expectedConstructorFile, Charset.defaultCharset());
      Assert.assertThat(constructorContent, Matchers.containsString("System.out"));
      Assert.assertThat(constructorContent, Matchers.not(Matchers.containsString("System.err")));
   }

   private void checkInitMethod(final File methodSourceFolder, final ChangedEntity initMethodEntity) throws IOException {
      File expectedInitFile = MethodChangeReader.getMethodDiffFile(methodSourceFolder, VERSION, initMethodEntity);
      String initContent = FileUtils.readFileToString(expectedInitFile, Charset.defaultCharset());
      Assert.assertThat(initContent, Matchers.not(Matchers.containsString("System.out")));
      Assert.assertThat(initContent, Matchers.containsString("System.err"));
   }
}
