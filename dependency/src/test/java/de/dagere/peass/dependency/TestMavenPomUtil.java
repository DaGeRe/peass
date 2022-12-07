package de.dagere.peass.dependency;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import de.dagere.peass.execution.maven.pom.MavenPomUtil;
import de.dagere.peass.execution.utils.EnvironmentVariables;


public class TestMavenPomUtil {
   
   @Test
   public void testGetProjectList() throws IOException {
      List<String> dependencyModuleDependents = MavenPomUtil.getDependentModules(new File(".."), "dependency", new EnvironmentVariables());
      System.out.println(dependencyModuleDependents);
      MatcherAssert.assertThat(dependencyModuleDependents, Matchers.contains("peass-parent", "dependency"));
      
      List<String> measurementModuleDependents = MavenPomUtil.getDependentModules(new File(".."), "measurement", new EnvironmentVariables());
      System.out.println(measurementModuleDependents);
      MatcherAssert.assertThat(measurementModuleDependents, Matchers.contains("peass-parent", "dependency", "measurement"));
      
      List<String> analysisModuleDependents = MavenPomUtil.getDependentModules(new File(".."), "analysis", new EnvironmentVariables());
      System.out.println(analysisModuleDependents);
      MatcherAssert.assertThat(analysisModuleDependents, Matchers.contains("peass-parent", "dependency", "measurement", "analysis"));
   }
}
