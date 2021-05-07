package de.dagere.peass.dependency;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.execution.GradleTestExecutor;
import de.dagere.peass.dependency.execution.MavenTestExecutor;
import de.dagere.peass.dependency.execution.TestExecutor;
import de.dagere.peass.dependency.jmh.JmhTestExecutor;
import de.dagere.peass.dependency.jmh.JMHTestTransformer;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.testtransformation.TestTransformer;

public class ExecutorCreator {
   
   private static final Logger LOG = LogManager.getLogger(ExecutorCreator.class);

   public static boolean hasBuildfile(final PeASSFolders folders) {
      final File pom = new File(folders.getProjectFolder(), "pom.xml");
      final File buildGradle = new File(folders.getProjectFolder(), "build.gradle");
      return pom.exists() || buildGradle.exists();
   }

   public static TestExecutor createExecutor(final PeASSFolders folders, final TestTransformer testTransformer, final EnvironmentVariables env) {
      final File pom = new File(folders.getProjectFolder(), "pom.xml");
      final File buildGradle = new File(folders.getProjectFolder(), "build.gradle");
      final String executorName = testTransformer.getConfig().getExecutionConfig().getTestExecutor();
      System.out.println(executorName);
      if (testTransformer != null && (!"default".equals(executorName) || testTransformer instanceof JMHTestTransformer)) {
         return new JmhTestExecutor(folders, testTransformer, env);
      } else if (buildGradle.exists()) {
         return new GradleTestExecutor(folders, (JUnitTestTransformer) testTransformer, env);
      } else if (pom.exists()) {
         return new MavenTestExecutor(folders, (JUnitTestTransformer) testTransformer, env);
      } else {
         throw new RuntimeException("No known buildfile (pom.xml  or build.gradle) in " + folders.getProjectFolder().getAbsolutePath() + " found; can not create test executor.");
      }
   }
   
   public static TestTransformer createTestTransformer(final PeASSFolders folders, final ExecutionConfig executionConfig, final MeasurementConfiguration measurementConfig) {
      TestTransformer transformer;
      try {
         Class<?> testTransformerClass = Class.forName(executionConfig.getTestTransformer());
         if (!Arrays.asList(testTransformerClass.getInterfaces()).contains(TestTransformer.class)) {
            throw new RuntimeException("TestTransformer needs to be implemented by " + executionConfig.getTestTransformer());
         }
         Constructor<?> constructor = testTransformerClass.getConstructor(File.class, MeasurementConfiguration.class);
         transformer = (TestTransformer) constructor.newInstance(folders.getProjectFolder(), measurementConfig);
      } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
         LOG.debug("Initialization was not possible; this should be thrown uncatched");
         e.printStackTrace();
         throw new RuntimeException(e);
      }
      return transformer;
   }

}
