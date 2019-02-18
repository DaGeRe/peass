package de.peass.dependencytests;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Assert;
import org.junit.Test;

import de.peass.dependency.execution.GradleParseUtil;

public class TestBuildGradle {

   private static final File CURRENT = new File(new File("target"), "current_gradle");

   @Test
   public void testNoUpdate() throws IOException {
      final File gradleFile = new File("src/test/resources/gradle/differentPlugin.gradle");

      final File destFile = new File(CURRENT, "build.gradle");
      FileUtils.copyFile(gradleFile, destFile);

      GradleParseUtil.addDependency(destFile, "de.dagere.kopeme:kopeme-junit:0.10-SNAPSHOT", "xyz");

      Assert.assertTrue(FileUtils.contentEquals(gradleFile, destFile));
   }

   @Test
   public void testBuildtoolUpdate() throws IOException {
      final File gradleFile = new File("src/test/resources/gradle/build.gradle");
      testUpdate(gradleFile, true);

      final File gradleFile2 = new File("src/test/resources/gradle/v2.gradle");
      testUpdate(gradleFile2, true);
   }

   @Test
   public void testAndroidLib() throws IOException {
      final File gradleFile3 = new File("src/test/resources/gradle/androidlib.gradle");
      testUpdate(gradleFile3, false);
   }

   public void testUpdate(final File gradleFile, final boolean buildtools) throws IOException {
      final File destFile = new File(CURRENT, "build.gradle");
      FileUtils.copyFile(gradleFile, destFile);

      GradleParseUtil.addDependency(destFile, "de.dagere.kopeme:kopeme-junit:0.10-SNAPSHOT", "xyz");

      final List<String> gradleFileContents = Files.readAllLines(Paths.get(destFile.toURI()));

      if (buildtools) {
         Assert.assertThat(gradleFileContents, IsCollectionContaining.hasItem(
               Matchers.anyOf(
                     Matchers.containsString("'buildTools': '19.1.0'"),
                     Matchers.containsString("buildToolsVersion 19.1.0"))));
      }
      

      Assert.assertThat(gradleFileContents, IsCollectionContaining.hasItem(Matchers.containsString("de.dagere.kopeme:kopeme-junit")));
   }
}
