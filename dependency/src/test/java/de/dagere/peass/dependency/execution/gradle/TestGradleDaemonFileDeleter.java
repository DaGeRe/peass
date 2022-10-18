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
}
