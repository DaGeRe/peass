package de.dagere.peass.dependency.execution.gradle;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.execution.gradle.GradleDaemonFileDeleter;

public class TestGradleDaemonFileDeleter {

   @Test
   public void testRegularDeletion() throws IOException {
      File exampleLogFile = new File("target/temp.out.log");
      FileUtils.touch(exampleLogFile);

      GradleDaemonFileDeleter.deleteDaemonFile(new File("src/test/resources/dependencyIT/gradle/example_log.out.log"));

      Assert.assertFalse(exampleLogFile.exists());
   }

   @Test
   public void testDeleteDaemonFile() throws IOException {
      File exampleLogFile = new File("target/daemon-122850.out.log");
      FileUtils.touch(exampleLogFile);

      GradleDaemonFileDeleter.deleteDaemonFile(new File("src/test/resources/dependencyIT/gradle/example_daemon.out.log"));
      Assert.assertFalse(exampleLogFile.exists());
   }
}
