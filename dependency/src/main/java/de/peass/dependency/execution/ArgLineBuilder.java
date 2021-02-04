package de.peass.dependency.execution;

import java.io.File;

import de.peass.testtransformation.JUnitTestTransformer;

public class ArgLineBuilder {

   public static final String TEMP_DIR = "-Djava.io.tmpdir";

   public static final String JAVA_AGENT = "-javaagent";

   public static final String KIEKER_FOLDER_MAVEN = "${user.home}/.m2/repository/net/kieker-monitoring/kieker/" + MavenTestExecutor.KIEKER_VERSION + "/kieker-"
         + MavenTestExecutor.KIEKER_VERSION + "-aspectj.jar";
   /**
    * This is added to surefire, assuming that kieker has been downloaded already, so that the aspectj-weaving can take place.
    */
   protected static final String KIEKER_ARG_LINE_MAVEN = JAVA_AGENT + ":" + KIEKER_FOLDER_MAVEN;

   public static final String KIEKER_FOLDER_GRADLE = "${System.properties['user.home']}/.m2/repository/net/kieker-monitoring/kieker/" + MavenTestExecutor.KIEKER_VERSION
         + "/kieker-" + MavenTestExecutor.KIEKER_VERSION + "-aspectj.jar";

   protected static final String KIEKER_ARG_LINE_GRADLE = JAVA_AGENT + ":" + KIEKER_FOLDER_GRADLE;

   private final JUnitTestTransformer testTransformer;

   public ArgLineBuilder(final JUnitTestTransformer testTransformer) {
      this.testTransformer = testTransformer;
   }

   public String buildArgline(final File tempFolder) {
      final String argline = buildGenericArgline(tempFolder, "=", " ", KIEKER_ARG_LINE_MAVEN);
      return argline;
   }

   private String buildGenericArgline(final File tempFolder, final String valueSeparator, final String entrySeparator, final String kiekerLine) {
      final String argline;
      if (testTransformer.getConfig().isUseKieker()) {
         String writerConfig;
         if (testTransformer.isAggregatedWriter()) {
            final String bulkFolder = "-Dkieker.monitoring.writer.filesystem.AggregatedTreeWriter.customStoragePath" + valueSeparator + tempFolder.getAbsolutePath();
            writerConfig = bulkFolder;
         } else {
            writerConfig = "";
         }

         if (!testTransformer.isAdaptiveExecution()) {
            if (testTransformer.getConfig().isUseSourceInstrumentation()) {
               argline = TEMP_DIR + valueSeparator + tempFolder.getAbsolutePath() +
                     entrySeparator + writerConfig;
            } else {
               argline = kiekerLine +
                     entrySeparator + TEMP_DIR + valueSeparator + tempFolder.getAbsolutePath() +
                     entrySeparator + writerConfig;
            }
         } else {
            if (testTransformer.getConfig().isUseSourceInstrumentation()) {
               argline = TEMP_DIR + valueSeparator + tempFolder.getAbsolutePath() +
                     entrySeparator + writerConfig;
            } else {
               argline = kiekerLine +
                     entrySeparator + TEMP_DIR + valueSeparator + tempFolder.getAbsolutePath() +
                     entrySeparator + writerConfig;
            }
         }
      } else {
         argline = "";
      }
      return argline;
   }

   public String buildArglineGradle(final File tempFolder) {
      final String argline = buildGenericArgline(tempFolder, ":", "\",\"", KIEKER_ARG_LINE_GRADLE);
      if (!argline.equals("")) {
         return "jvmArgs=[\"" + argline.substring(0, argline.length() - 2) + "\"]";
      } else {
         return argline;
      }
   }
}
