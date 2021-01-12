package de.peass.dependencyprocessors;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class TestUtil {
   
   public static void deleteOldFolders() {
      final File resultFolder = new File("target/current_peass/");
      if (resultFolder.exists()) {
         try {
            for (final File subdir : resultFolder.listFiles()) {
               if (subdir.isDirectory()) {
                  FileUtils.deleteDirectory(subdir);
               }
            }
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }
   }
}
