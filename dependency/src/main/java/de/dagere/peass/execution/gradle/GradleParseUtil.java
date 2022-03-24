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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.execution.maven.pom.MavenPomUtil;

public class GradleParseUtil {

   private static final Logger LOG = LogManager.getLogger(GradleParseUtil.class);

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

   public static FindDependencyVisitor setAndroidTools(final File buildfile) {
      FindDependencyVisitor visitor = null;
      try {
         LOG.debug("Editing: {}", buildfile);

         visitor = parseBuildfile(buildfile);
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
   
   public static void removeExclusions(final FindDependencyVisitor visitor) {
      for (Integer lineNumber : visitor.getExcludeLines()) {
         visitor.clearLine(lineNumber);
      }
   }

   public static FindDependencyVisitor parseBuildfile(final File buildfile) throws IOException, FileNotFoundException {
      FindDependencyVisitor visitor = new FindDependencyVisitor(buildfile);
      return visitor;

   }

   public static void updateBuildTools(final FindDependencyVisitor visitor) {
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

   public static void updateBuildToolsVersion(final FindDependencyVisitor visitor) {
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

   public static void addJUnitVersionSpringBoot(final FindDependencyVisitor visitor) {
      visitor.getLines().add("ext['junit-jupiter.version']='" + MavenPomUtil.JUPITER_VERSION + "'");
   }
}
