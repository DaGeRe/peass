package de.dagere.peass.dependency;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.execution.TestExecutor;
import de.dagere.peass.execution.gradle.GradleTestExecutor;
import de.dagere.peass.execution.maven.pom.MavenTestExecutor;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.testtransformation.TestTransformer;

public class ExecutorCreator {

   private static final Logger LOG = LogManager.getLogger(ExecutorCreator.class);

   public static boolean hasBuildfile(final PeassFolders folders, final TestTransformer testTransformer) {
      TestExecutor dummyExecutor = createExecutor(folders, testTransformer, new EnvironmentVariables());
      return dummyExecutor.doesBuildfileExist();
   }

   public static TestExecutor createExecutor(final PeassFolders folders, final TestTransformer testTransformer, final EnvironmentVariables env) {
      final String executorName = testTransformer.getConfig().getExecutionConfig().getTestExecutor();
      LOG.info(executorName);
      if (testTransformer != null && !executorName.equals("default")) {
         return createDefinedExecutor(folders, testTransformer, env, executorName);
      } else if (executorName.equals("default")) {
         return createDefaultExecutor(folders, testTransformer, env);
      } else {
         throw new RuntimeException("Executor creation did not work, executor name: " + executorName);
      }
   }

   private static TestExecutor createDefinedExecutor(final PeassFolders folders, final TestTransformer testTransformer, final EnvironmentVariables env, final String executorName) {
      try {
         Class<?> executorClazz = Class.forName(executorName);
         if (!ConstructorFinder.isSubclass(executorClazz, TestExecutor.class)) {
            throw new RuntimeException("Clazz " + executorName + " was given as executor, but no (direct) subclass of TestExecutor!");
         }
         Constructor<?> constructor = ConstructorFinder.findConstructor(executorClazz);
         // Since static type checking is impossible here, this might throw exceptions if no matching instances are given
         TestExecutor transformer = (TestExecutor) constructor.newInstance(folders, testTransformer, env);
         return transformer;
      } catch (ClassNotFoundException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
            | InvocationTargetException e) {
         throw new RuntimeException("Executor creation did not work, executor name: " + executorName, e);
      }
   }

   

   private static TestExecutor createDefaultExecutor(final PeassFolders folders, final TestTransformer testTransformer, final EnvironmentVariables env) {
      final File pom = new File(folders.getProjectFolder(), "pom.xml");
      final File buildGradle = new File(folders.getProjectFolder(), "build.gradle");
      if (buildGradle.exists()) {
         return new GradleTestExecutor(folders, (JUnitTestTransformer) testTransformer, env);
      } else if (pom.exists()) {
         return new MavenTestExecutor(folders, (JUnitTestTransformer) testTransformer, env);
      } else {
         throw new RuntimeException("No known buildfile (pom.xml  or build.gradle) in " + folders.getProjectFolder().getAbsolutePath() + " found; can not create test executor.");
      }
   }

   public static TestTransformer createTestTransformer(final PeassFolders folders, final ExecutionConfig executionConfig, final MeasurementConfig measurementConfig) {
      try {
         Class<?> testTransformerClass = Class.forName(executionConfig.getTestTransformer());
         if (!Arrays.asList(testTransformerClass.getInterfaces()).contains(TestTransformer.class)) {
            throw new RuntimeException("TestTransformer needs to be implemented by " + executionConfig.getTestTransformer());
         }
         Constructor<?> constructor = testTransformerClass.getConstructor(File.class, MeasurementConfig.class);
         TestTransformer transformer = (TestTransformer) constructor.newInstance(folders.getProjectFolder(), measurementConfig);
         return transformer;
      } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
            | InvocationTargetException e) {
         LOG.debug("Initialization was not possible; this should be thrown uncatched");
         e.printStackTrace();
         throw new RuntimeException(e);
      }

   }

   public static TestTransformer createTestTransformer(final PeassFolders folders, final ExecutionConfig executionConfig, final KiekerConfig kiekerConfig) {
      try {
         Class<?> testTransformerClass = Class.forName(executionConfig.getTestTransformer());
         if (!Arrays.asList(testTransformerClass.getInterfaces()).contains(TestTransformer.class)) {
            throw new RuntimeException("TestTransformer needs to be implemented by " + executionConfig.getTestTransformer());
         }
         Constructor<?> constructor = testTransformerClass.getConstructor(File.class, ExecutionConfig.class, KiekerConfig.class);
         TestTransformer transformer = (TestTransformer) constructor.newInstance(folders.getProjectFolder(), executionConfig, kiekerConfig);
         return transformer;
      } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException
            | InvocationTargetException e) {
         LOG.debug("Initialization was not possible; this should be thrown uncatched");
         e.printStackTrace();
         throw new RuntimeException(e);
      }
   }
}
