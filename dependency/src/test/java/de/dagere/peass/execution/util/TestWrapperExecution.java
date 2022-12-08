package de.dagere.peass.execution.util;

import java.io.File;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.execution.utils.EnvironmentVariables;

public class TestWrapperExecution {

   private static final File EXAMPLE_PROJECTS = new File("src/test/resources/maven-wrapper-example");

   @Test
   public void testWrapper() {
      EnvironmentVariables environmentVariables = new EnvironmentVariables();
      String mvnCall = environmentVariables.fetchMavenCall(new File(EXAMPLE_PROJECTS, "with-wrapper"));
      if (EnvironmentVariables.isLinux()) {
         Assert.assertEquals("./mvnw", mvnCall);
      } else {
         Assert.assertEquals("mvnw.cmd", mvnCall);
      }
   }

   @Test
   public void testNoWrapper() {
      EnvironmentVariables environmentVariables = new EnvironmentVariables();
      String mvnCall = environmentVariables.fetchMavenCall(new File(EXAMPLE_PROJECTS, "no-wrapper"));
      if (EnvironmentVariables.isLinux()) {
         Assert.assertEquals("mvn", mvnCall);
      } else {
         Assert.assertEquals("mvn.cmd", mvnCall);
      }
   }
}
