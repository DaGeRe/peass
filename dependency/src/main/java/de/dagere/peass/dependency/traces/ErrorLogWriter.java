package de.dagere.peass.dependency.traces;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.analysis.data.TestCase;

/**
 * Since GitHub actions fails in one test without the option to step into the problem, and since this is not reproducible using GitHubs local docker container, this class tries to
 * find the log folder and print an example log.
 * 
 * @author reichelt
 *
 */
public class ErrorLogWriter {

   private static final Logger LOG = LogManager.getLogger(ErrorLogWriter.class);

   private final TestCase testcase;
   private final File resultsFolder;

   public ErrorLogWriter(final TestCase testcase, final File resultsFolder) {
      this.testcase = testcase;
      this.resultsFolder = resultsFolder;
   }

   public void tryToWriteLastLog() {
      File logFolder = new File(resultsFolder, "../../logs/");
      LOG.info("Searching in" + logFolder.getAbsolutePath());
      if (logFolder.exists()) {
         writeLogFolderContent(logFolder);
      } else {
         File testingFolder = new File(resultsFolder, "../");
         int index = 0;
         while (!testingFolder.exists() && index < 10) {
            LOG.info("Folder " + testingFolder.getAbsolutePath() + " did not exist");
            testingFolder = new File(testingFolder.getAbsolutePath(), "..");
            index++;
         }
         if (testingFolder.exists()) {
            LOG.info("Files in " + testingFolder.getAbsolutePath());
            for (File file : testingFolder.listFiles()) {
               System.out.println(file.getAbsolutePath());
            }
         } else {
            LOG.info("Did no succeed searching in parent folders; start to search from root");
            String[] parts = resultsFolder.getAbsolutePath().split(File.separator);
            File userFolder = new File(File.separator + parts[0] + File.separator + parts[1]);
            System.out.println(userFolder.getAbsolutePath() + " " + userFolder.exists());
            for (int i = 2; i < parts.length; i++) {
               userFolder = new File(userFolder, parts[i]);
               System.out.println(userFolder.getAbsolutePath() + " " + userFolder.exists());
               if (userFolder.getName().endsWith("_peass")) {
                  File foundLogFolder = new File(userFolder, "logs");
                  if (foundLogFolder.exists()) {
                     writeLogFolderContent(foundLogFolder);
                  } else {
                     LOG.info("Expected log folder {} did not exist", foundLogFolder);
                  }

               }
            }
         }
      }
   }

   private void writeLogFolderContent(final File logFolder) {
      File[] logFiles = logFolder.listFiles(new FileFilter() {
         @Override
         public boolean accept(final File pathname) {
            return pathname.isDirectory();
         }
      });
      if (logFiles != null && logFiles.length > 0) {
         File logFolderChild = logFiles[0];
         LOG.info("First folder: " + logFolderChild.getAbsolutePath());
         File txtFile = new File(logFolderChild, "log_" + testcase.getClazz() + "/" + testcase.getMethod() + ".txt");
         LOG.info("Trying " + txtFile.getAbsolutePath() + " " + txtFile.exists());
         if (txtFile.exists()) {
            try {
               System.out.println(FileUtils.readFileToString(txtFile, StandardCharsets.UTF_8));
            } catch (IOException e) {
               e.printStackTrace();
            }
         }
      } else {
         LOG.info("Logfolder was empty");
      }
   }

}
