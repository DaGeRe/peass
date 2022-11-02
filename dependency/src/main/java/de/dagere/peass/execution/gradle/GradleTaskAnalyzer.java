package de.dagere.peass.execution.gradle;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.execution.utils.CommandConcatenator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.execution.utils.ProjectModules;
import de.dagere.peass.utils.StreamGobbler;

/**
 * Gradle tasks can only be derived by with high effort by parsing buildfiles. Therefore, this class derives tasks (and thereby used plugins) by starting gradle.
 * 
 * @author DaGeRe
 *
 */
public class GradleTaskAnalyzer {

   private static final Logger LOG = LogManager.getLogger(GradleTaskAnalyzer.class);

   private final boolean isJava;
   private final boolean isSpring;
   private final boolean isIntegrationTest;
   private final boolean isAndroid;
   private final ProjectModules modules;

   public GradleTaskAnalyzer(File moduleFolder, File projectFolder, EnvironmentVariables env) throws IOException {
      String wrapper = new File(projectFolder, EnvironmentVariables.fetchGradleCall()).getAbsolutePath();

      String[] envPropertyArray = env.getProperties().length() > 0 ? env.getProperties().split(" ") : new String[0];
      final String[] varsWithProperties = CommandConcatenator.concatenateCommandArrays(new String[] { wrapper, "tasks", "--all" }, envPropertyArray);
      LOG.debug("Command: {}", Arrays.toString(varsWithProperties));

      ProcessBuilder processBuilder = new ProcessBuilder(varsWithProperties);
      processBuilder.directory(moduleFolder);

      for (Map.Entry<String, String> entry : env.getEnvironmentVariables().entrySet()) {
         LOG.trace("Environment: {} = {}", entry.getKey(), entry.getValue());
         processBuilder.environment().put(entry.getKey(), entry.getValue());
      }

      Process process = processBuilder.start();
      String processOutput = StreamGobbler.getFullProcess(process, true);

      GradleDaemonFileDeleter.deleteDaemonFile(processOutput);

      LOG.debug(processOutput);

      List<String> taskLines = Arrays.stream(processOutput.split("\n"))
            .filter(line -> line.contains(" - "))
            .collect(Collectors.toList());

      isAndroid = taskLines.stream().anyMatch(line -> line.startsWith("androidDependencies ")) || taskLines.stream().anyMatch(line -> line.startsWith("installDebug "));
      isSpring = taskLines.stream().anyMatch(line -> line.startsWith("bootJar ")) || taskLines.stream().anyMatch(line -> line.startsWith("bootWar "));

      isJava = (taskLines.stream().anyMatch(line -> line.startsWith("jar ")) && taskLines.stream().anyMatch(line -> line.startsWith("test ")))
            || isAndroid
            || isSpring;

      isIntegrationTest = taskLines.stream().anyMatch(line -> line.startsWith("integrationTest"));

      LinkedList<File> moduleFiles = new LinkedList<>();
      taskLines.stream()
            .filter(line -> line.contains("compileJava "))
            .forEach(line -> {
               String firstPart = line.substring(0, line.indexOf(" - "));
               if (firstPart.equals("compileJava")) {
                  moduleFiles.add(moduleFolder);
               } else {
                  String subModuleName = firstPart
                        .substring(0, firstPart.length() - "compileJava".length())
                        .replace(":", File.separator);
                  File subModuleFile = new File(moduleFolder, subModuleName);
                  if (subModuleFile.exists()) {
                     moduleFiles.add(subModuleFile);
                  }
               }
            });

      modules = new ProjectModules(moduleFiles);
   }

   public GradleTaskAnalyzer(File moduleFolder, EnvironmentVariables env) throws IOException {
      this(moduleFolder, moduleFolder, env);
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

   public ProjectModules getModules() {
      return modules;
   }
}
