package de.peass.dependency;

import java.io.File;

import de.peass.dependency.execution.EnvironmentVariables;
import de.peass.dependency.execution.GradleTestExecutor;
import de.peass.dependency.execution.MavenTestExecutor;
import de.peass.dependency.execution.TestExecutor;
import de.peass.testtransformation.JUnitTestTransformer;

public class ExecutorCreator {
   
   public static boolean hasBuildfile(final PeASSFolders folders) {
      final File pom = new File(folders.getProjectFolder(), "pom.xml");
      final File buildGradle = new File(folders.getProjectFolder(), "build.gradle");
      return pom.exists() || buildGradle.exists();
   }

   public static TestExecutor createExecutor(final PeASSFolders folders, final JUnitTestTransformer testTransformer, final EnvironmentVariables env) {
      final File pom = new File(folders.getProjectFolder(), "pom.xml");
      final File buildGradle = new File(folders.getProjectFolder(), "build.gradle");
      if (buildGradle.exists()) {
         return new GradleTestExecutor(folders, testTransformer, env);
      } else if (pom.exists()) {
         return new MavenTestExecutor(folders, testTransformer, env);
      } else {
         throw new RuntimeException("No known buildfile (pom.xml  or build.gradle) in " + folders.getProjectFolder().getAbsolutePath() + " found; can not create test executor.");
      }
   }

}
