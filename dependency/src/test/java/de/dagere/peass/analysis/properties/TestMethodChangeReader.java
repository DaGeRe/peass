package de.dagere.peass.analysis.properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;

public class TestMethodChangeReader {

   public static final File methodSourceFolder = new File("target" + File.separator + "current_sources");
   public static final String VERSION = "000001";
   public static final ExecutionConfig TEST_CONFIG = new ExecutionConfig();
   static {
      TEST_CONFIG.setCommit(VERSION);
   }

   public TestMethodChangeReader() {
      System.out.println("Contains System.out");
   }

   public void init() {
      System.err.println("Contains System.err");
   }

   @BeforeEach
   public void before() {
      if (!methodSourceFolder.exists()) {
         methodSourceFolder.mkdirs();
      }
   }

   @Test
   public void testSourceWriting() throws FileNotFoundException, IOException {
      ChangedEntity constructorEntity = writeConstructor(new File("."), methodSourceFolder);
      ChangedEntity initMethodEntity = writeInit(new File("."), methodSourceFolder);

      checkConstructor(methodSourceFolder, constructorEntity);
      checkInitMethod(methodSourceFolder, initMethodEntity);
   }

   public static ChangedEntity writeInit(final File sourceFolder, final File methodSourceFolder) throws FileNotFoundException, IOException {
      ChangedEntity initMethodEntity = new ChangedEntity("de.dagere.peass.analysis.properties.TestMethodChangeReader", "", "init");
      MethodChangeReader reader2 = new MethodChangeReader(methodSourceFolder, sourceFolder,
            new File("."), initMethodEntity, VERSION, TEST_CONFIG);
      reader2.readMethodChangeData();
      return initMethodEntity;
   }

   public static ChangedEntity writeConstructor(final File sourceFolder, final File methodSourceFolder) throws FileNotFoundException, IOException {
      ChangedEntity constructorEntity = new ChangedEntity("de.dagere.peass.analysis.properties.TestMethodChangeReader", "", "<init>");
      MethodChangeReader reader = new MethodChangeReader(methodSourceFolder, sourceFolder,
            new File("."), constructorEntity, VERSION, TEST_CONFIG);
      reader.readMethodChangeData();
      return constructorEntity;
   }

   private void checkConstructor(final File methodSourceFolder, final ChangedEntity constructorEntity) throws IOException {
      File expectedConstructorFile = MethodChangeReader.getMethodDiffFile(methodSourceFolder, VERSION, constructorEntity);
      String constructorContent = FileUtils.readFileToString(expectedConstructorFile, Charset.defaultCharset());
      MatcherAssert.assertThat(constructorContent, Matchers.containsString("System.out"));
      MatcherAssert.assertThat(constructorContent, Matchers.not(Matchers.containsString("System.err")));
   }

   private void checkInitMethod(final File methodSourceFolder, final ChangedEntity initMethodEntity) throws IOException {
      File expectedInitFile = MethodChangeReader.getMethodDiffFile(methodSourceFolder, VERSION, initMethodEntity);
      String initContent = FileUtils.readFileToString(expectedInitFile, Charset.defaultCharset());
      MatcherAssert.assertThat(initContent, Matchers.not(Matchers.containsString("System.out")));
      MatcherAssert.assertThat(initContent, Matchers.containsString("System.err"));
   }
}
