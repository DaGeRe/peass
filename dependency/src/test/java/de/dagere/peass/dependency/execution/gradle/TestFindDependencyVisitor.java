package de.dagere.peass.dependency.execution.gradle;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.execution.gradle.FindDependencyVisitor;

public class TestFindDependencyVisitor {
   
   private static final File GRADLE_FOLDER = new File("src/test/resources/gradle-buildfile-unittest");
   
   @Test
   public void testWithPlugins() throws IOException {
      File withApplyPlugins = new File(GRADLE_FOLDER, "with-apply.gradle");
      File buildfile = new File("target/build.gradle");
      FileUtils.copyFile(withApplyPlugins, buildfile);
      
      FindDependencyVisitor visitor = new FindDependencyVisitor(buildfile);
      Assert.assertTrue(visitor.isUseJava());
   }
   
   @Test
   public void testPluginsSection() throws IOException {
      File withApplyPlugins = new File(GRADLE_FOLDER, "with-plugins-section.gradle");
      File buildfile = new File("target/build.gradle");
      FileUtils.copyFile(withApplyPlugins, buildfile);
      
      FindDependencyVisitor visitor = new FindDependencyVisitor(buildfile);
      Assert.assertTrue(visitor.isUseJava());
   }
}
