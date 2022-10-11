package de.dagere.peass.execution.gradle;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.ExecutionConfig;
import de.dagere.peass.execution.maven.pom.MavenPomUtil;

import static de.dagere.peass.execution.gradle.GradleBuildfileVisitor.*;

public class GradleParseUtil {

   private static final Logger LOG = LogManager.getLogger(GradleParseUtil.class);
   private static final String JUPITER_EXECUTION_CONFIG_CONCURRENT = "junit.jupiter.execution.parallel.mode.default";
   private static final String JUPITER_EXECUTION_CONFIG_CONCURRENT_VALUE = "SAME_THREAD";

   private static final String JUPITER_EXECUTION_CONFIG_MODE = "junit.jupiter.execution.parallel.enabled";
   private static final String JUPITER_EXECUTION_CONFIG_MODE_VALUE = "false";

   public static void writeInitGradle(final File init) {
      if (!init.exists()) {
         try (FileWriter fw = new FileWriter(init)) {
            final PrintWriter pw = new PrintWriter(fw);
            pw.write("allprojects{");
            pw.write(" repositories {");
            pw.write("  mavenLocal();");
            pw.write("  maven { url 'https://maven.google.com' };");
            fw.write(" }");
            fw.write("}");
            pw.flush();
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }
   }

   public static GradleBuildfileVisitor setAndroidTools(final File buildfile, ExecutionConfig config) {
      GradleBuildfileVisitor visitor = null;
      try {
         LOG.debug("Editing: {}", buildfile);

         visitor = parseBuildfile(buildfile, config);
         final List<String> gradleFileContents = Files.readAllLines(Paths.get(buildfile.toURI()));

         if (visitor.getBuildTools() != -1) {
            updateBuildTools(visitor);
         }

         if (visitor.getBuildToolsVersion() != -1) {
            updateBuildToolsVersion(visitor);
         }

         Files.write(buildfile.toPath(), gradleFileContents, StandardCharsets.UTF_8);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return visitor;
   }

   public static void removeExclusions(final GradleBuildfileVisitor visitor) {
      for (Integer lineNumber : visitor.getExcludeLines()) {
         visitor.clearLine(lineNumber);
      }
   }

   public static GradleBuildfileVisitor parseBuildfile(final File buildfile, ExecutionConfig config) throws IOException, FileNotFoundException {
      GradleBuildfileVisitor visitor = new GradleBuildfileVisitor(buildfile, config);
      return visitor;

   }

   public static void updateBuildTools(final GradleBuildfileVisitor visitor) {
      final int lineIndex = visitor.getBuildTools() - 1;
      final String versionLine = visitor.getLines().get(lineIndex).trim().replaceAll("'", "").replace("\"", "");
      final String versionString = versionLine.split(":")[1].trim();
      if (AndroidVersionUtil.isLegalBuildTools(versionString)) {
         final String runningVersion = AndroidVersionUtil.getRunningVersion(versionString);
         if (runningVersion != null) {
            visitor.getLines().set(lineIndex, "'buildTools': '" + runningVersion + "'");
         } else {
            visitor.setHasVersion(false);
         }
      }
   }

   public static void updateBuildToolsVersion(final GradleBuildfileVisitor visitor) {
      final int lineIndex = visitor.getBuildToolsVersion() - 1;
      final String versionLine = visitor.getLines().get(lineIndex).trim().replaceAll("'", "").replace("\"", "");
      final String versionString = versionLine.split(" ")[1].trim();
      if (AndroidVersionUtil.isLegalBuildToolsVersion(versionString)) {
         LOG.info(lineIndex + " " + versionLine);
         final String runningVersion = AndroidVersionUtil.getRunningVersion(versionString);
         if (runningVersion != null) {
            visitor.getLines().set(lineIndex, "buildToolsVersion " + runningVersion);
         } else {
            visitor.setHasVersion(false);
         }
      }
   }

   public static void addJUnitVersionSpringBoot(final GradleBuildfileVisitor visitor) {
      visitor.getLines().add("ext['junit-jupiter.version']='" + MavenPomUtil.JUPITER_VERSION + "'");
   }

   public static void updateExecutionMode(GradleBuildfileVisitor visitor) {

      for (Map.Entry<String, Integer> entry : visitor.getTestExecutionProperties().entrySet()) {
         updateExecutionProperties(visitor, entry, visitor.hasTestSystemPropertiesBlock());
      }

      for (Map.Entry<String, Integer> entry : visitor.getIntegrationtestExecutionProperties().entrySet()) {
         updateExecutionProperties(visitor, entry, visitor.hasIntegrationTestSystemPropertiesBlock());
      }
   }

   private static void updateExecutionProperties(GradleBuildfileVisitor visitor, Map.Entry<String, Integer> entry, boolean hasSystemPropertiesBlock) {
      int value = entry.getValue();
      if (entry.getKey().equals(JUPITER_EXECUTION_CONFIG)) {
         visitor.clearLine(value);
         visitor.addLine(value, createTextForAdding(JUPITER_EXECUTION_CONFIG_MODE, JUPITER_EXECUTION_CONFIG_MODE_VALUE, hasSystemPropertiesBlock));
      } else if (entry.getKey().equals(JUPITER_EXECUTION_CONFIG_DEFAULT)) {
         visitor.clearLine(value);
         visitor.addLine(value, createTextForAdding(JUPITER_EXECUTION_CONFIG_CONCURRENT, JUPITER_EXECUTION_CONFIG_CONCURRENT_VALUE, hasSystemPropertiesBlock));
      }
   }

   public static String createTextForAdding(String key, String value, boolean hasSystemPropertiesBlock) {
      String textProperty = hasSystemPropertiesBlock ? "'" : "  systemProperty   '";
      String separator = hasSystemPropertiesBlock ? "'             : '" : "'             , '";
      String lineSeparator = hasSystemPropertiesBlock ? "'," : "'";

      return textProperty + key + separator + value + lineSeparator;
   }

}
