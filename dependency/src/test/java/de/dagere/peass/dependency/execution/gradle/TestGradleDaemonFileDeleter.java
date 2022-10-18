package de.dagere.peass.dependency.execution.gradle;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.execution.gradle.GradleDaemonFileDeleter;

public class TestGradleDaemonFileDeleter {

   File exampleLogFile = new File("target/temp.out.log");
   
   @Test
   public void testRegularDeletion() throws IOException {
      FileUtils.touch(exampleLogFile);

      GradleDaemonFileDeleter.deleteDaemonFile(new File("src/test/resources/dependencyIT/gradle/example_log.out.log"));

      Assert.assertFalse(exampleLogFile.exists());
   }

   @Test
   public void testDeleteDaemonFile() throws IOException {
      FileUtils.touch(exampleLogFile);

      GradleDaemonFileDeleter.deleteDaemonFile(new File("src/test/resources/dependencyIT/gradle/example_daemon.out.log"));
      Assert.assertFalse(exampleLogFile.exists());
   }
   
   @Test
   public void testDeletionByString() throws IOException {
      String exampleString = "Initialized native services in: /home/reichelt/.gradle/native\n"
            + "The client will now receive all logging from the daemon (pid: 66429). The daemon log file: target/temp.out.log\n"
            + "Starting 5th build in daemon [uptime: 4 mins 37.012 secs, performance: 96%, non-heap usage: 22% of 268,4 MB]\n";
      FileUtils.touch(exampleLogFile);
      
      GradleDaemonFileDeleter.deleteDaemonFile(exampleString);
      Assert.assertFalse(exampleLogFile.exists());
   }
}
