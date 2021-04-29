package de.dagere.peass.dependency.reader;

import java.io.File;

import de.dagere.peass.dependency.PeASSFolders;

public class TooBigLogCleaner {
   public static final int MAX_SIZE_IN_MB = 10;

   public static void cleanTooBigLogs(final PeASSFolders folders, final String version) {
      File logFolder = folders.getLogFolder();
      File versionFolder = new File(logFolder, version);
      if (versionFolder.exists()) {
         for (File clazzFolder : versionFolder.listFiles()) {
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
