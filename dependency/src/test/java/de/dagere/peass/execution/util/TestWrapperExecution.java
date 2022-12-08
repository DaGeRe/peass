package de.dagere.peass.execution.util;

import java.io.File;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.execution.utils.EnvironmentVariables;

public class TestWrapperExecution {

   private static final File EXAMPLE_PROJECTS = new File("src/test/resources/maven-wrapper-example");
   public static final File WITH_WRAPPER = new File(EXAMPLE_PROJECTS, "with-wrapper");
   public static final File NO_WRAPPER = new File(EXAMPLE_PROJECTS, "no-wrapper");

   @Test
   public void testWrapper() {
      EnvironmentVariables environmentVariables = new EnvironmentVariables();
      String mvnCall = environmentVariables.fetchMavenCall(new File(EXAMPLE_PROJECTS, "with-wrapper"));
      if (EnvironmentVariables.isLinux()) {
         Assert.assertEquals("./mvnw", mvnCall);
      } else {
         MatcherAssert.assertThat(mvnCall, Matchers.endsWith("mvnw.cmd"));
      }
   }

   @Test
   public void testNoWrapper() {
      EnvironmentVariables environmentVariables = new EnvironmentVariables();
      String mvnCall = environmentVariables.fetchMavenCall(NO_WRAPPER);
      if (EnvironmentVariables.isLinux()) {
         Assert.assertEquals("mvn", mvnCall);
      } else {
         Assert.assertEquals("mvn.cmd", mvnCall);
      }
   }
}
