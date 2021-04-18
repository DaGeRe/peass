package de.peass.dependency.execution;

import java.io.File;

import de.peass.testtransformation.JUnitTestTransformer;

public class ArgLineBuilder {

   public static final String TEMP_DIR = "-Djava.io.tmpdir";

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

   private final JUnitTestTransformer testTransformer;
   private final File modulePath;

   public ArgLineBuilder(final JUnitTestTransformer testTransformer, final File modulePath) {
      this.testTransformer = testTransformer;
      this.modulePath = modulePath;
   }

   public String buildArgline(final File tempFolder) {
      final String argline = buildGenericArgline(tempFolder, "=", " ", KIEKER_ARG_LINE_MAVEN);
      return argline;
   }

   private String buildGenericArgline(final File tempFolder, final String valueSeparator, final String entrySeparator, final String kiekerLine) {
      String argline;
      if (testTransformer.getConfig().isUseKieker()) {
         String writerConfig;
         String tempFolderPath = "'" + tempFolder.getAbsolutePath() + "'";
         if (testTransformer.isAggregatedWriter()) {
            final String bulkFolder = "-D" + AOPXMLHelper.AGGREGATED_WRITER + ".customStoragePath" + valueSeparator + tempFolderPath;
            writerConfig = bulkFolder;
         } else {
            writerConfig = "";
         }

         if (!testTransformer.getConfig().isEnableAdaptiveConfig()) {
            if (testTransformer.getConfig().isUseSourceInstrumentation()) {
               argline = TEMP_DIR + valueSeparator + tempFolderPath;
               if (!writerConfig.equals("")) {
                  argline += entrySeparator + writerConfig;
               }

            } else {
               argline = kiekerLine +
                     entrySeparator + TEMP_DIR + valueSeparator + tempFolderPath +
                     entrySeparator + writerConfig;
            }
         } else {
            if (testTransformer.getConfig().isUseSourceInstrumentation()) {
               argline = TEMP_DIR + valueSeparator + tempFolderPath +
                     entrySeparator + writerConfig;
            } else {
               argline = kiekerLine +
                     entrySeparator + TEMP_DIR + valueSeparator + tempFolderPath +
                     entrySeparator + writerConfig;
            }
         }
         if (!entrySeparator.contains("\"")) {
            argline += " -Dkieker.monitoring.configuration" + valueSeparator + "\"" + modulePath.getAbsolutePath() + "/src/main/resources/META-INF/kieker.monitoring.properties\"";
         } else {
            argline += " -Dkieker.monitoring.configuration" + valueSeparator + "'" + modulePath.getAbsolutePath() + "/src/main/resources/META-INF/kieker.monitoring.properties'";
         }
      } else {
         argline = "";
      }
      return argline;
   }

   public String buildArglineGradle(final File tempFolder) {
      final String argline = buildGenericArgline(tempFolder, ":", "\",\"", KIEKER_ARG_LINE_GRADLE);
      if (!argline.equals("")) {
         String fullArgLine = "\"" + argline + "\"";
         return "jvmArgs=[" + fullArgLine + "]";
      } else {
         return argline;
      }
   }
}
