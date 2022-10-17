package de.dagere.peass.dependency.execution.gradle;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.execution.gradle.GradleBuildfileVisitor;
import de.dagere.peass.execution.gradle.GradleTaskAnalyzer;
import de.dagere.peass.execution.utils.EnvironmentVariables;

public class TestGradleBuildfileVisitor {
   
   public static final File GRADLE_FOLDER = new File("src/test/resources/gradle-buildfile-unittest");
   
   @Test
   public void testWithPlugins() throws IOException {
      File withApplyPlugins = new File(GRADLE_FOLDER, "with-apply.gradle");
      File buildfile = GradleTestUtil.initProject(withApplyPlugins);

      GradleTaskAnalyzer executor = new GradleTaskAnalyzer(buildfile.getParentFile(), new EnvironmentVariables());
      Assert.assertTrue(executor.isUseJava());
   }
   
   @Test
   public void testApplication() throws IOException {
      File withApplyPlugins = new File(GRADLE_FOLDER, "build-application.gradle");
      File buildfile = GradleTestUtil.initProject(withApplyPlugins);
      
      GradleTaskAnalyzer executor = new GradleTaskAnalyzer(buildfile.getParentFile(), new EnvironmentVariables());
      Assert.assertTrue(executor.isUseJava());
   }
   
   @Test
   public void testPluginsSection() throws IOException {
      File withApplyPlugins = new File(GRADLE_FOLDER, "with-plugins-section.gradle");
      File buildfile = GradleTestUtil.initProject(withApplyPlugins);
      
      GradleTaskAnalyzer executor = new GradleTaskAnalyzer(buildfile.getParentFile(), new EnvironmentVariables());
      Assert.assertTrue(executor.isUseJava());
   }
   
   @Test
   public void testImportGrgit() throws IOException {
      File withApplyPlugins = new File(GRADLE_FOLDER, "build_grgit.gradle");
      File buildfile = GradleTestUtil.initProject(withApplyPlugins);
      
      GradleTaskAnalyzer executor = new GradleTaskAnalyzer(buildfile.getParentFile(), new EnvironmentVariables());
      Assert.assertTrue(executor.isUseJava());
   }
   
   @Test
   public void testExcludeJUnit4() throws IOException {
      File withApplyPlugins = new File(GRADLE_FOLDER, "build_exclude_JUnit4.gradle");
      File buildfile = GradleTestUtil.initProject(withApplyPlugins);
      
      GradleBuildfileVisitor visitor = new GradleBuildfileVisitor(buildfile, new ExecutionConfig());
      Assert.assertEquals(9, visitor.getExcludeLines().get(0).intValue());
   }
   
   @Test
   public void testExcludeJUnit5() throws IOException {
      File withApplyPlugins = new File(GRADLE_FOLDER, "build_exclude_JUnit5.gradle");
      File buildfile = GradleTestUtil.initProject(withApplyPlugins);
      
      GradleBuildfileVisitor visitor = new GradleBuildfileVisitor(buildfile, new ExecutionConfig());
      Assert.assertEquals(9, visitor.getExcludeLines().get(0).intValue());
   }
}
