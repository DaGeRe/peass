package de.dagere.peass.execution.gradle;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.utils.StreamGobbler;

/**
 * Gradle tasks can only be derived by with high effort by parsing buildfiles. Therefore, this class derives tasks (and thereby used plugins) by starting gradle.
 * 
 * @author DaGeRe
 *
 */
public class GradleTaskAnalyzer {

   private final boolean isJava;
   private final boolean isSpring;
   private final boolean isIntegrationTest;
   private final boolean isAndroid;

   public GradleTaskAnalyzer(File moduleFolder, File projectFolder) throws IOException {
      String wrapper = new File(projectFolder, EnvironmentVariables.fetchGradleCall()).getAbsolutePath();
      ProcessBuilder processBuilder = new ProcessBuilder(wrapper, "tasks", "--all");
      processBuilder.directory(moduleFolder);

      Process process = processBuilder.start();
      String processOutput = StreamGobbler.getFullProcess(process, true);

      List<String> taskLines = Arrays.stream(processOutput.split("\n"))
            .filter(line -> line.contains(" - "))
            .collect(Collectors.toList());

      isAndroid = taskLines.stream().anyMatch(line -> line.startsWith("androidDependencies ")) || taskLines.stream().anyMatch(line -> line.startsWith("installDebug "));
      isSpring = taskLines.stream().anyMatch(line -> line.startsWith("bootJar ")) || taskLines.stream().anyMatch(line -> line.startsWith("bootWar "));

      isJava = (taskLines.stream().anyMatch(line -> line.startsWith("jar ")) && taskLines.stream().anyMatch(line -> line.startsWith("test ")))
            || isAndroid
            || isSpring;

      isIntegrationTest = taskLines.stream().anyMatch(line -> line.startsWith("integrationTest "));

   }
   
   public GradleTaskAnalyzer(File moduleFolder) throws IOException {
      this(moduleFolder, moduleFolder);
   }

   public boolean isUseJava() {
      return isJava;
   }

   public boolean isUseSpringBoot() {
      return isSpring;
   }

   public boolean isIntegrationTest() {
      return isIntegrationTest;
   }

   public boolean isAndroid() {
      return isAndroid;
   }
}
