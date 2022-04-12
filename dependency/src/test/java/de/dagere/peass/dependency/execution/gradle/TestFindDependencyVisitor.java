package de.dagere.peass.dependency.execution.gradle;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.execution.gradle.GradleBuildfileVisitor;

public class TestFindDependencyVisitor {
   
   public static final File GRADLE_FOLDER = new File("src/test/resources/gradle-buildfile-unittest");
   
   @Test
   public void testWithPlugins() throws IOException {
      File withApplyPlugins = new File(GRADLE_FOLDER, "with-apply.gradle");
      File buildfile = new File("target/build.gradle");
      FileUtils.copyFile(withApplyPlugins, buildfile);
      
      GradleBuildfileVisitor visitor = new GradleBuildfileVisitor(buildfile, new ExecutionConfig());
      Assert.assertTrue(visitor.isUseJava());
   }
   
   @Test
   public void testCustomJavaPlugin() throws IOException {
      File withApplyPlugins = new File(GRADLE_FOLDER, "build_customJavaPlugin.gradle");
      File buildfile = new File("target/build.gradle");
      FileUtils.copyFile(withApplyPlugins, buildfile);
      
      ExecutionConfig config = new ExecutionConfig();
      config.setGradleJavaPluginName("abc.java;abc.jaxb");
      GradleBuildfileVisitor visitor = new GradleBuildfileVisitor(buildfile, config);
      Assert.assertTrue(visitor.isUseJava());
   }
   
   @Test
   public void testApplication() throws IOException {
      File withApplyPlugins = new File(GRADLE_FOLDER, "build-application.gradle");
      File buildfile = new File("target/build.gradle");
      FileUtils.copyFile(withApplyPlugins, buildfile);
      
      GradleBuildfileVisitor visitor = new GradleBuildfileVisitor(buildfile, new ExecutionConfig());
      Assert.assertTrue(visitor.isUseJava());
   }
   
   @Test
   public void testPluginsSection() throws IOException {
      File withApplyPlugins = new File(GRADLE_FOLDER, "with-plugins-section.gradle");
      File buildfile = new File("target/build.gradle");
      FileUtils.copyFile(withApplyPlugins, buildfile);
      
      GradleBuildfileVisitor visitor = new GradleBuildfileVisitor(buildfile, new ExecutionConfig());
      Assert.assertTrue(visitor.isUseJava());
   }
   
   @Test
   public void testImportGrgit() throws IOException {
      File withApplyPlugins = new File(GRADLE_FOLDER, "build_grgit.gradle");
      File buildfile = new File("target/build.gradle");
      FileUtils.copyFile(withApplyPlugins, buildfile);
      
      GradleBuildfileVisitor visitor = new GradleBuildfileVisitor(buildfile, new ExecutionConfig());
      Assert.assertTrue(visitor.isUseJava());
   }
}
