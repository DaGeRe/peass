package de.dagere.peass.execution.gradle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GradleDaemonFileDeleter {

   private static final Logger LOG = LogManager.getLogger(GradleDaemonFileDeleter.class);

   public static void deleteDaemonFile(final File regularLogFile) {
      try (final BufferedReader reader = new BufferedReader(new FileReader(regularLogFile))) {
         String daemonFilename = findDaemonFilename(reader);
         deleteDaemonFileByName(daemonFilename);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public static void deleteDaemonFile(final String processOutput) {
      try (final BufferedReader reader = new BufferedReader(new StringReader(processOutput))) {
         String daemonFilename = findDaemonFilename(reader);
         deleteDaemonFileByName(daemonFilename);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   private static boolean isLogfile(final String daemonFilename) {
      if (daemonFilename != null) {
         return daemonFilename.endsWith(".out.log");
      } else {
         LOG.debug("daemonFilename could not be determined (is null)!");
         return false;
      }
   }

   private static String findDaemonFilename(final BufferedReader reader) throws IOException {
      final String searchString = "The client will now receive all logging from the daemon (pid: ";
      final int maxSearchLines = 50;
      String daemonLine = "";
      for (int i = 0; i < maxSearchLines; i++) {
         daemonLine = reader.readLine();
         if (daemonLine.contains(searchString)) {
            return daemonLine.substring(daemonLine.lastIndexOf(" ") + 1);
         }
      }
      LOG.debug("pid could not be found in first {} lines!", maxSearchLines);
      return null;
   }

   private static void deleteDaemonFileByName(final String daemonFilename) {
      if (isLogfile(daemonFilename)) {
         final File daemonFile = new File(daemonFilename);
         if (daemonFile.exists()) {
            daemonFile.delete();
         }
      } else {
         LOG.debug("{} does not seem to be a gradle-daemon logfile (does not end with \".out.log\") or is null, so it was not deleted!", daemonFilename);
      }
   }
}
