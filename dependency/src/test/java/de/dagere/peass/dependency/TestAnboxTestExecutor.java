package de.dagere.peass.dependency;

import java.io.File;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.execution.gradle.AnboxTestExecutor;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;

public class TestAnboxTestExecutor {

   public static final File temporaryFolder = new File("target/current");
   public static AnboxTestExecutor executor;
   public static PeassFolders peassFolders;
   public static JUnitTestTransformer testTransformer;
   public static EnvironmentVariables env;

   @BeforeAll
   public static void initAnboxTestExecutor() {
      peassFolders = new PeassFolders(temporaryFolder);
      testTransformer = new JUnitTestTransformer(temporaryFolder, new MeasurementConfig(2));
      env = new EnvironmentVariables();
      executor = new AnboxTestExecutor(peassFolders, testTransformer, env);
   }

   @Test
   public void testGetTestPackageName(){
      TestMethodCall test;
      
      test = new TestMethodCall("my.package.Clazz", "method");
      Assert.assertEquals("my.package.test", executor.getTestPackageName(test));

      test = new TestMethodCall("my.package.test.Clazz", "method");
      Assert.assertEquals("my.package.test", executor.getTestPackageName(test));
   }

   @Test
   public void testGetTestPackageNameDefaultNull(){
      Assert.assertEquals(null, executor.getTestPackageName());
   }

   @Test
   public void testGetTestPackageNameOverride(){
      TestMethodCall test;
      
      executor.getTestTransformer().getConfig().getExecutionConfig().setAndroidTestPackageName("com.mypackage.android.app.test");
      test = new TestMethodCall("my.package.Clazz", "method");
      Assert.assertEquals("com.mypackage.android.app.test", executor.getTestPackageName());
      Assert.assertEquals("com.mypackage.android.app.test", executor.getTestPackageName(test));
   }
}
