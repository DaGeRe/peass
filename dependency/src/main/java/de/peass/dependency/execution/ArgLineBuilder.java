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
   protected static final String KIEKER_ARG_LINE = JAVA_AGENT + ":" + KIEKER_FOLDER_MAVEN;

   private final JUnitTestTransformer testTransformer;

   public ArgLineBuilder(final JUnitTestTransformer testTransformer) {
      this.testTransformer = testTransformer;
   }

   public String buildArgline(final File tempFile) {
      final String argline;
      if (testTransformer.getConfig().isUseKieker()) {
         String writerConfig;
         if (testTransformer.isAggregatedWriter()) {
            final String bulkFolder = "-Dkieker.monitoring.writer.filesystem.AggregatedTreeWriter.customStoragePath=" + tempFile.getAbsolutePath().toString();
            writerConfig = bulkFolder;
         } else {
            writerConfig = "";
         }

         if (!testTransformer.isAdaptiveExecution()) {
            if (testTransformer.getConfig().isUseSourceInstrumentation()) {
               argline = TEMP_DIR + "=" + tempFile.getAbsolutePath().toString() +
                     " " + writerConfig;
            } else {
               argline = KIEKER_ARG_LINE +
                     " " + TEMP_DIR + "=" + tempFile.getAbsolutePath().toString() +
                     " " + writerConfig;
            }
         } else {
            if (testTransformer.getConfig().isUseSourceInstrumentation()) {
               argline = TEMP_DIR + "=" + tempFile.getAbsolutePath().toString() +
                     " " + writerConfig;
            } else {
               argline = KIEKER_ARG_LINE +
                     " " + TEMP_DIR + "=" + tempFile.getAbsolutePath().toString() +
                     " " + writerConfig;
            }
         }
      } else {
         argline = "";
      }
      return argline;
   }
}
