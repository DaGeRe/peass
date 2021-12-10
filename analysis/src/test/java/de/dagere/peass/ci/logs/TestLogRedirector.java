package de.dagere.peass.ci.logs;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.ci.logHandling.LogRedirector;

public class TestLogRedirector {

   private static final Logger LOG = LogManager.getLogger(TestLogRedirector.class);

   @BeforeEach
   public void setup() throws IOException {
      LOG.info("Deleting files");
      LogRedirectionTestFiles.outerLogFile.delete();
      LogRedirectionTestFiles.logFile.delete();
      LogRedirectionTestFiles.logFile2.delete();
   }
   
   @Test
   public void testRedirection() throws IOException {
      executeSomeOutputStuff();

      try (BufferedReader reader = new BufferedReader(new FileReader(LogRedirectionTestFiles.logFile))) {
         String line1 = reader.readLine();
         Assert.assertEquals("Should go to file", line1);
         String line2 = reader.readLine();
         MatcherAssert.assertThat(line2, Matchers.containsString("test logging - should also go to file"));
      }

      try (BufferedReader reader = new BufferedReader(new FileReader(LogRedirectionTestFiles.logFile2))) {
         String line1 = reader.readLine();
         Assert.assertEquals("Should go to file2", line1);
         String line2 = reader.readLine();
         MatcherAssert.assertThat(line2, Matchers.containsString("test - should go to file2"));
      }

      try (BufferedReader reader = new BufferedReader(new FileReader(LogRedirectionTestFiles.outerLogFile))) {
         String line1 = reader.readLine();
         MatcherAssert.assertThat(line1, Matchers.containsString("TestA - should go to outer file"));
         String line2 = reader.readLine();
         MatcherAssert.assertThat(line2, Matchers.containsString("TestB - should go to outer file"));
         String line3 = reader.readLine();
         MatcherAssert.assertThat(line3, Matchers.containsString("Test C - Should go to outer file"));
         String line4 = reader.readLine();
         MatcherAssert.assertThat(line4, Matchers.containsString("TestD - should go to outer file"));
      }
   }

   

   private void executeSomeOutputStuff() throws FileNotFoundException {
      try (LogRedirector outer = new LogRedirector(LogRedirectionTestFiles.outerLogFile)) {
         LOG.debug("TestA - should go to outer file");
         try (LogRedirector director = new LogRedirector(LogRedirectionTestFiles.logFile)) {
            System.out.println("Should go to file");
            LOG.debug("test logging - should also go to file");
         }
         LOG.debug("TestB - should go to outer file");
         System.out.println("Test C - Should go to outer file");

         try (LogRedirector director = new LogRedirector(LogRedirectionTestFiles.logFile2)) {
            System.out.println("Should go to file2");
            LOG.debug("test - should go to file2");
         }
         LOG.debug("TestD - should go to outer file");
      }
   }
}
