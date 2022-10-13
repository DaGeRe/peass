package de.dagere.peass.execution.gradle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class GradleDaemonFileDeleter {
   public static void deleteDaemonFile(File regularLogFile) {
      try (BufferedReader reader = new BufferedReader(new FileReader(regularLogFile))) {
         reader.readLine();
         String daemonLine = reader.readLine();
         String daemonFileName = daemonLine.substring(daemonLine.lastIndexOf(" ") + 1);
         File daemonFile = new File(daemonFileName);
         if (daemonFile.exists()) {
            daemonFile.delete();
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
