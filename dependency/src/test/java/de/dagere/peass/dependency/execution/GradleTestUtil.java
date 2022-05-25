package de.dagere.peass.dependency.execution;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class GradleTestUtil {

   public static final File GRADLE_WRAPPER_FOLDER = new File("src/test/resources/gradle-test-wrapper");

   public static File initProject(File originalBuildfile, File projectTestDirectory) throws IOException {
      FileUtils.copyDirectory(GRADLE_WRAPPER_FOLDER, projectTestDirectory);
      File buildfile = new File(projectTestDirectory, "build.gradle");
      FileUtils.copyFile(originalBuildfile, buildfile);
      return buildfile;
   }

   public static File initProject(File originalBuildfile) throws IOException {
      File projectTestDirectory = new File("target/buildfile-test");
      return initProject(originalBuildfile, projectTestDirectory);
   }

   public static void initWrapper(File target) throws IOException {
      for (File content : GradleTestUtil.GRADLE_WRAPPER_FOLDER.listFiles()) {
         File dest = new File(target, content.getName());
         if (content.isDirectory()) {
            FileUtils.copyDirectory(content, dest);
         } else {
            FileUtils.copyFile(content, dest);
         }
      }
   }
}
