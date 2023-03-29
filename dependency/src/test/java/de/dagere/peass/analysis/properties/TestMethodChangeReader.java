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

import de.dagere.nodeDiffGenerator.data.MethodCall;
import de.dagere.peass.config.ExecutionConfig;

public class TestMethodChangeReader {

   public static final File methodSourceFolder = new File("target" + File.separator + "current_sources");
   public static final String COMMIT = "000001";
   public static final ExecutionConfig TEST_CONFIG = new ExecutionConfig();

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
      MethodCall constructorEntity = writeConstructor(new File("."), methodSourceFolder);
      MethodCall initMethodEntity = writeInit(new File("."), methodSourceFolder);

      checkConstructor(methodSourceFolder, constructorEntity);
      checkInitMethod(methodSourceFolder, initMethodEntity);
   }

   public static MethodCall writeInit(final File sourceFolder, final File methodSourceFolder) throws FileNotFoundException, IOException {
      MethodCall initMethodEntity = new MethodCall("de.dagere.peass.analysis.properties.TestMethodChangeReader", "", "init");
      MethodChangeReader reader2 = new MethodChangeReader(methodSourceFolder, sourceFolder,
            new File("."), initMethodEntity, COMMIT, TEST_CONFIG);
      reader2.readMethodChangeData();
      return initMethodEntity;
   }

   public static MethodCall writeConstructor(final File sourceFolder, final File methodSourceFolder) throws FileNotFoundException, IOException {
      MethodCall constructorEntity = new MethodCall("de.dagere.peass.analysis.properties.TestMethodChangeReader", "", "<init>");
      MethodChangeReader reader = new MethodChangeReader(methodSourceFolder, sourceFolder,
            new File("."), constructorEntity, COMMIT, TEST_CONFIG);
      reader.readMethodChangeData();
      return constructorEntity;
   }

   private void checkConstructor(final File methodSourceFolder, final MethodCall constructorEntity) throws IOException {
      File expectedConstructorFile = new ChangedMethodManager(methodSourceFolder).getMethodDiffFile(COMMIT, constructorEntity);
      String constructorContent = FileUtils.readFileToString(expectedConstructorFile, Charset.defaultCharset());
      MatcherAssert.assertThat(constructorContent, Matchers.containsString("System.out"));
      MatcherAssert.assertThat(constructorContent, Matchers.not(Matchers.containsString("System.err")));
   }

   private void checkInitMethod(final File methodSourceFolder, final MethodCall initMethodEntity) throws IOException {
      File expectedInitFile = new ChangedMethodManager(methodSourceFolder).getMethodDiffFile(COMMIT, initMethodEntity);
      String initContent = FileUtils.readFileToString(expectedInitFile, Charset.defaultCharset());
      MatcherAssert.assertThat(initContent, Matchers.not(Matchers.containsString("System.out")));
      MatcherAssert.assertThat(initContent, Matchers.containsString("System.err"));
   }
}
