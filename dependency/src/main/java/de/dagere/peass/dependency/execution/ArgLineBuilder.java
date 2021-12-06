package de.dagere.peass.dependency.execution;

import java.io.File;

import de.dagere.peass.dependency.execution.pom.MavenPomUtil;
import de.dagere.peass.testtransformation.TestTransformer;

public class ArgLineBuilder {

   public static final String TEMP_DIR_PURE = "java.io.tmpdir";
   public static final String TEMP_DIR = "-D" + TEMP_DIR_PURE;
   private static final String KIEKER_CONFIGURATION_PURE = "kieker.monitoring.configuration";
   private static final String KIEKER_CONFIGURATION = "-D" + KIEKER_CONFIGURATION_PURE;
   private static final String MONITORING_PROPERTIES_PATH = "/src/main/resources/META-INF/kieker.monitoring.properties";

   public static final String JAVA_AGENT = "-javaagent";

   public static final String RELATIVE_MAVEN_FOLDER = ".m2" + File.separator + "repository" + File.separator + "net" + File.separator
         + "kieker-monitoring" + File.separator + "kieker" + File.separator + MavenPomUtil.KIEKER_VERSION + File.separator + "kieker-"
         + MavenPomUtil.KIEKER_VERSION + "-aspectj.jar";
   public static final String KIEKER_FOLDER_MAVEN = "${user.home}" + File.separator + RELATIVE_MAVEN_FOLDER;
   /**
    * This is added to surefire, assuming that kieker has been downloaded already, so that the aspectj-weaving can take place.
    */
   protected static final String KIEKER_ARG_LINE_MAVEN = JAVA_AGENT + ":" + KIEKER_FOLDER_MAVEN;

   public static final String KIEKER_FOLDER_GRADLE = "${System.properties['user.home']}" + File.separator + RELATIVE_MAVEN_FOLDER;

   protected static final String KIEKER_ARG_LINE_GRADLE = JAVA_AGENT + ":" + KIEKER_FOLDER_GRADLE;

   private final TestTransformer testTransformer;
   private final File modulePath;

   public ArgLineBuilder(final TestTransformer testTransformer, final File modulePath) {
      this.testTransformer = testTransformer;
      this.modulePath = modulePath;
   }

   public String buildArgline(final File tempFolder) {
      final String argline = buildGenericArgline(tempFolder, "=", " ", KIEKER_ARG_LINE_MAVEN);
      return argline;
   }

   private String buildGenericArgline(final File tempFolder, final String valueSeparator, final String entrySeparator, final String kiekerLine) {
      String argline = getTieredCompilationArglinePart(entrySeparator);
      if (testTransformer.getConfig().isUseKieker()) {
         final String tempFolderPath = "'" + tempFolder.getAbsolutePath() + "'";
         if (testTransformer.getConfig().getKiekerConfig().isUseSourceInstrumentation()) {
            argline += TEMP_DIR + valueSeparator + tempFolderPath;

         } else {
            argline += kiekerLine + entrySeparator + TEMP_DIR + valueSeparator + tempFolderPath;
         }
         if (!entrySeparator.contains("\"")) {
            argline += " " + KIEKER_CONFIGURATION + valueSeparator + "\"" + modulePath.getAbsolutePath() + MONITORING_PROPERTIES_PATH + "\"";
         } else {
            argline += entrySeparator + KIEKER_CONFIGURATION + valueSeparator + "'" + modulePath.getAbsolutePath()
                  + MONITORING_PROPERTIES_PATH + "'";
         }
      }
      return argline;
   }

   private String getTieredCompilationArglinePart(final String entrySeparator) {
      String argline;
      if (testTransformer.getConfig().getExecutionConfig().isUseTieredCompilation()) {
         argline = "-XX:-TieredCompilation" + entrySeparator;
      } else {
         argline = "";
      }
      return argline;
   }

   public String buildArglineGradle(final File tempFolder) {
      // final String argline = buildGenericArgline(tempFolder, ":", "\",\"", KIEKER_ARG_LINE_GRADLE);
      if (testTransformer.getConfig().isUseKieker()) {
         String tempPathNoEscapes = tempFolder.getAbsolutePath().replace('\\', '/');
         String argLine = "  systemProperty \"" + TEMP_DIR_PURE + "\", \"" + tempPathNoEscapes + "\"" + System.lineSeparator();
         String configFilePath = modulePath.getAbsolutePath().replace('\\', '/') + MONITORING_PROPERTIES_PATH;
         argLine += "  systemProperty \"" + KIEKER_CONFIGURATION_PURE + "\", \"" + configFilePath + "\"" + System.lineSeparator();
         if (!testTransformer.getConfig().getKiekerConfig().isUseSourceInstrumentation()) {
            argLine += "  jvmArgs=[\"" + KIEKER_ARG_LINE_GRADLE + "\"]" + System.lineSeparator();
         }
         return argLine;
      } else {
         return "";
      }
   }
}
