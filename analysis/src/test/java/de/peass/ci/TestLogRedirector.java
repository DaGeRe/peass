package de.peass.ci;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

public class TestLogRedirector {

   private static final Logger LOG = LogManager.getLogger(TestLogRedirector.class);

   private final File outerLogFile = new File("target/outer.txt");
   private final File logFile = new File("target/test.txt");
   private final File logFile2 = new File("target/test2.txt");

   @Test
   public void testRedirection() throws IOException {
      executeSomeOutputStuff();

      try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
         String line1 = reader.readLine();
         Assert.assertEquals("Should go to file", line1);
         String line2 = reader.readLine();
         MatcherAssert.assertThat(line2, Matchers.containsString("test logging - should also go to file"));
      }

      try (BufferedReader reader = new BufferedReader(new FileReader(logFile2))) {
         String line1 = reader.readLine();
         Assert.assertEquals("Should go to file2", line1);
         String line2 = reader.readLine();
         MatcherAssert.assertThat(line2, Matchers.containsString("test - should go to file2"));
      }
      
      try (BufferedReader reader = new BufferedReader(new FileReader(outerLogFile))) {
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
      try (LogRedirector outer = new LogRedirector(outerLogFile)) {
         LOG.debug("TestA - should go to outer file");
         try (LogRedirector director = new LogRedirector(logFile)) {
            System.out.println("Should go to file");
            LOG.debug("test logging - should also go to file");
         }
         LOG.debug("TestB - should go to outer file");
         System.out.println("Test C - Should go to outer file");

         try (LogRedirector director = new LogRedirector(logFile2)) {
            System.out.println("Should go to file2");
            LOG.debug("test - should go to file2");
         }
         LOG.debug("TestD - should go to outer file");
      }
   }
}
