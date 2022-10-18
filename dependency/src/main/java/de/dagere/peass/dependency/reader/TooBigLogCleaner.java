package de.dagere.peass.dependency.reader;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import de.dagere.peass.dependency.KiekerResultManager;
import de.dagere.peass.folders.PeassFolders;

public class TooBigLogCleaner {
   public static final int MAX_SIZE_IN_MB = 10;

   public static void cleanJSONFolder(final PeassFolders folders) throws IOException {
      final File xmlFileFolder = KiekerResultManager.getJSONFileFolder(folders, folders.getProjectFolder());
      if (xmlFileFolder != null) {
         FileUtils.deleteDirectory(xmlFileFolder);
      }
   }

   public static void cleanTooBigLogs(final PeassFolders folders, final String version) {

      File[] logFolders = new File[] { folders.getDependencyLogFolder(), folders.getMeasureLogFolder(), folders.getTreeLogFolder(), folders.getRCALogFolder() };
      for (File logFolder : logFolders) {
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
}
