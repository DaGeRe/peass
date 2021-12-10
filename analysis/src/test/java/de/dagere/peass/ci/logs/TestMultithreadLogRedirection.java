package de.dagere.peass.ci.logs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.io.Files;

import de.dagere.peass.ci.logHandling.LogRedirector;

public class TestMultithreadLogRedirection {
   
   private static final Logger LOG = LogManager.getLogger(TestMultithreadLogRedirection.class);
   
   @BeforeEach
   public void setup() throws IOException {
      LOG.info("Deleting files");
      LogRedirectionTestFiles.outerLogFile.delete();
      LogRedirectionTestFiles.logFile.delete();
      LogRedirectionTestFiles.logFile2.delete();
   }
   
   @Test
   public void testSubthreadRedirections() throws InterruptedException, IOException {
      createMultithreadLogs();
      
      System.out.println("This should go to regular console again");

      List<String> logFile2Content = Files.readLines(LogRedirectionTestFiles.logFile2, StandardCharsets.UTF_8);
      Assert.assertEquals(logFile2Content.get(0), "Other first log");
      MatcherAssert.assertThat(logFile2Content.get(1), Matchers.containsString("Other log4j log"));
      Assert.assertEquals(logFile2Content.get(2), "Half second over");
      MatcherAssert.assertThat(logFile2Content.get(3), Matchers.containsString("Other log4j over log"));
      
      List<String> logFileContent = Files.readLines(LogRedirectionTestFiles.logFile, StandardCharsets.UTF_8);
      Assert.assertEquals(logFileContent.get(0), "First log");
      MatcherAssert.assertThat(logFileContent.get(1), Matchers.containsString("First log4j log"));
      Assert.assertEquals(logFileContent.get(2), "Half second over first");
      MatcherAssert.assertThat(logFileContent.get(3), Matchers.containsString("First log4j over log"));
   }

   private void createMultithreadLogs() throws InterruptedException {
      Runnable innerRunnable = new Runnable() {

         @Override
         public void run() {
            try (LogRedirector redirector = new LogRedirector(LogRedirectionTestFiles.logFile)) {
               System.out.println("First log");
               LOG.info("First log4j log");
               Thread.sleep(500);
               System.out.println("Half second over first");
               LOG.info("First log4j over log");
            } catch (FileNotFoundException | InterruptedException e) {
               e.printStackTrace();
            }

         }
      };

      Runnable outerRunnable = new Runnable() {

         @Override
         public void run() {
            try (LogRedirector redirector = new LogRedirector(LogRedirectionTestFiles.logFile2)) {
               System.out.println("Other first log");
               LOG.info("Other log4j log");
               Thread.sleep(500);
               System.out.println("Half second over");
               LOG.info("Other log4j over log");
            } catch (FileNotFoundException | InterruptedException e) {
               e.printStackTrace();
            }

         }
      };
      
      Thread outer = new Thread(outerRunnable);
      Thread inner = new Thread(innerRunnable);
      outer.start();
      Thread.sleep(250);
      inner.start();
      
      outer.join();
      inner.join();
   }
}
