package de.dagere.peass.ci.logs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

      List<String> outerFileContent = Files.readLines(LogRedirectionTestFiles.outerLogFile, StandardCharsets.UTF_8);
      Assert.assertEquals(outerFileContent.get(0), "Outer first log");
      Assert.assertEquals(outerFileContent.get(1), "Outer second over");
   }

   private void createMultithreadLogs() throws InterruptedException {
      Runnable innerRunnable = new Runnable() {

         @Override
         public void run() {
            try (LogRedirector redirector = new LogRedirector(LogRedirectionTestFiles.logFile)) {
               System.out.println("First log");
               Thread.sleep(500);
               System.out.println("One second over");
            } catch (FileNotFoundException | InterruptedException e) {
               e.printStackTrace();
            }

         }
      };

      Runnable outerRunnable = new Runnable() {

         @Override
         public void run() {
            try (LogRedirector redirector = new LogRedirector(LogRedirectionTestFiles.outerLogFile)) {
               System.out.println("Outer first log");
               Thread.sleep(500);
               System.out.println("Outer second over");
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
