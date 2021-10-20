package de.dagere.peass.dependency;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import de.dagere.peass.dependency.execution.pom.MavenPomUtil;

public class TestMavenPomUtil {
   
   @Test
   public void testGetProjectList() throws IOException {
      List<String> dependencyModuleDependents = MavenPomUtil.getDependentModules(new File(".."), "dependency");
      System.out.println(dependencyModuleDependents);
      MatcherAssert.assertThat(dependencyModuleDependents, Matchers.contains("peass-parent", "dependency"));
      
      List<String> measurementModuleDependents = MavenPomUtil.getDependentModules(new File(".."), "measurement");
      System.out.println(measurementModuleDependents);
      MatcherAssert.assertThat(measurementModuleDependents, Matchers.contains("peass-parent", "dependency", "measurement"));
      
      List<String> analysisModuleDependents = MavenPomUtil.getDependentModules(new File(".."), "analysis");
      System.out.println(analysisModuleDependents);
      MatcherAssert.assertThat(analysisModuleDependents, Matchers.contains("peass-parent", "dependency", "measurement", "analysis"));
   }
}
