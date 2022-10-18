package de.dagere.peass.ci.logs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringStartsWith;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.ci.logHandling.LogRedirector;

public class TestLogRedirectorToStream {

   private static final Logger LOG = LogManager.getLogger(TestLogRedirectorToStream.class);

   @Test
   public void testRedirection() throws IOException {
      final ByteArrayOutputStream outerStream = new ByteArrayOutputStream();
      final ByteArrayOutputStream stream1 = new ByteArrayOutputStream();
      final ByteArrayOutputStream stream2 = new ByteArrayOutputStream();

      executeSomeOutputStuff(outerStream, stream1, stream2);

      String[] content1 = stream1.toString().split("\n");
      MatcherAssert.assertThat(content1[0], StringStartsWith.startsWith("Should go to file"));
      MatcherAssert.assertThat(content1[1], Matchers.containsString("test logging - should also go to file"));

      String[] content2 = stream2.toString().split("\n");
      MatcherAssert.assertThat(content2[0], StringStartsWith.startsWith("Should go to file2"));
      MatcherAssert.assertThat(content2[1], Matchers.containsString("test - should go to file2"));

      String[] contentOuter = outerStream.toString().split("\n");
      MatcherAssert.assertThat(contentOuter[0], Matchers.containsString("TestA - should go to outer file"));
      MatcherAssert.assertThat(contentOuter[1], Matchers.containsString("TestB - should go to outer file"));
      MatcherAssert.assertThat(contentOuter[2], Matchers.containsString("Test C - Should go to outer file"));
      MatcherAssert.assertThat(contentOuter[3], Matchers.containsString("TestD - should go to outer file"));
   }

   private void executeSomeOutputStuff(final ByteArrayOutputStream outerStream, final ByteArrayOutputStream stream1, final ByteArrayOutputStream stream2) {
      try (LogRedirector outer = new LogRedirector(new PrintStream(outerStream))) {
         LOG.debug("TestA - should go to outer file");
         try (LogRedirector director = new LogRedirector(new PrintStream(stream1))) {
            System.out.println("Should go to file");
            LOG.debug("test logging - should also go to file");
         }
         LOG.debug("TestB - should go to outer file");
         System.out.println("Test C - Should go to outer file");

         try (LogRedirector director = new LogRedirector(new PrintStream(stream2))) {
            System.out.println("Should go to file2");
            LOG.debug("test - should go to file2");
         }
         LOG.debug("TestD - should go to outer file");
      }
   }
}
