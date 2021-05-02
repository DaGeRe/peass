package de.dagere.peass.dependency;

import java.io.File;

import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.dependency.execution.GradleTestExecutor;
import de.dagere.peass.dependency.execution.MavenTestExecutor;
import de.dagere.peass.dependency.execution.TestExecutor;
import de.dagere.peass.dependency.jmh.JMHTestExecutor;
import de.dagere.peass.testtransformation.JUnitTestTransformer;
import de.dagere.peass.testtransformation.TestTransformer;

public class ExecutorCreator {

   public static boolean hasBuildfile(final PeASSFolders folders) {
      final File pom = new File(folders.getProjectFolder(), "pom.xml");
      final File buildGradle = new File(folders.getProjectFolder(), "build.gradle");
      return pom.exists() || buildGradle.exists();
   }

   public static TestExecutor createExecutor(final PeASSFolders folders, final TestTransformer testTransformer, final EnvironmentVariables env) {
      final File pom = new File(folders.getProjectFolder(), "pom.xml");
      final File buildGradle = new File(folders.getProjectFolder(), "build.gradle");
      if (!"default".equals(testTransformer.getConfig().getExecutionConfig().getTestExecutor())) {
         return new JMHTestExecutor(folders, testTransformer, env);
      } else if (buildGradle.exists()) {
         return new GradleTestExecutor(folders, (JUnitTestTransformer) testTransformer, env);
      } else if (pom.exists()) {
         return new MavenTestExecutor(folders, (JUnitTestTransformer) testTransformer, env);
      } else {
         throw new RuntimeException("No known buildfile (pom.xml  or build.gradle) in " + folders.getProjectFolder().getAbsolutePath() + " found; can not create test executor.");
      }
   }

}
