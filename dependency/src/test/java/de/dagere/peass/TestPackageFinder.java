package de.dagere.peass;

import java.io.File;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsIterableContaining;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.dependency.ClazzFileFinder;

public class TestPackageFinder {

   @Test
   public void testDependencyModule() {
      ExecutionConfig config = new ExecutionConfig();
      config.getClazzFolders().add("src/main/java");
      config.getClazzFolders().add("src/java");
      
      final List<String> lowestPackage = new ClazzFileFinder(config).getClasses(new File("."));
      System.out.println(lowestPackage);
      MatcherAssert.assertThat(lowestPackage, IsIterableContaining.hasItem("de.dagere.peass.SelectStarter"));
      MatcherAssert.assertThat(lowestPackage, Matchers.not(IsIterableContaining.hasItem("de.dagere.peass.SelectStarter.SelectStarter")));
      MatcherAssert.assertThat(lowestPackage, IsIterableContaining.hasItem("de.dagere.peass.dependency.statistics.DependencyStatisticAnalyzer"));
      MatcherAssert.assertThat(lowestPackage, IsIterableContaining.hasItem("de.dagere.peass.dependency.statistics.DependencyStatistics"));
      MatcherAssert.assertThat(lowestPackage, IsIterableContaining.hasItem("de.dagere.peass.TestPackageFinder"));
   }
}
