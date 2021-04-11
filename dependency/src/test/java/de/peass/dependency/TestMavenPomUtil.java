package de.peass.dependency;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import de.peass.dependency.execution.MavenPomUtil;

public class TestMavenPomUtil {
   
   @Test
   public void testGetProjectList() throws IOException {
      List<String> dependencyModuleDependents = MavenPomUtil.getDependentModules(new File(".."), "dependency");
      System.out.println(dependencyModuleDependents);
      Assert.assertThat(dependencyModuleDependents, Matchers.contains("peass-parent", "dependency"));
      
      List<String> measurementModuleDependents = MavenPomUtil.getDependentModules(new File(".."), "measurement");
      System.out.println(measurementModuleDependents);
      Assert.assertThat(measurementModuleDependents, Matchers.contains("peass-parent", "dependency", "measurement"));
      
      List<String> analysisModuleDependents = MavenPomUtil.getDependentModules(new File(".."), "analysis");
      System.out.println(analysisModuleDependents);
      Assert.assertThat(analysisModuleDependents, Matchers.contains("peass-parent", "dependency", "measurement", "analysis"));
   }
}
