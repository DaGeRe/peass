package de.dagere.peass.dependency.reader;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.dependency.KiekerResultManager;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.folders.VMExecutionLogFolders;

public class TooBigLogCleaner {
   public static final int MAX_SIZE_IN_MB = 10;

   public static void cleanJSONFolder(final PeassFolders folders) throws IOException {
      final File jsonFileFolder = KiekerResultManager.getJSONFileFolder(folders, folders.getProjectFolder());
      if (jsonFileFolder != null) {
         FileUtils.deleteDirectory(jsonFileFolder);
      }
   }

   public static void cleanTooBigLogs(final PeassFolders folders, final String commit) {
      VMExecutionLogFolders vmLogFolders = folders.getLogFolders();
      File[] logFolders = vmLogFolders.getExistingLogFolders();
      for (File logFolder : logFolders) {
         File commitFolder = new File(logFolder, commit);
         if (commitFolder.exists()) {
            for (File clazzFolder : commitFolder.listFiles()) {
               if (clazzFolder.isDirectory()) {
                  for (File methodLog : clazzFolder.listFiles()) {
                     long sizeInMb = (methodLog.length() / (1024 * 1024));
                     if (sizeInMb > MAX_SIZE_IN_MB) {
                        methodLog.delete();
                     }
                  }
               }
            }
         }
      }
   }
}
