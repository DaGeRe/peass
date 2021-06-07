package de.dagere.peass;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public class TestUtil {

   public static final File CURRENT_PEASS = new File("target/current_peass/");

   public static void deleteOldFolders() {
      deleteContents(CURRENT_PEASS);
   }
   
   public static void deleteContents(final File folder) {
      if (folder.exists()) {
         try {
            for (final File subfile : folder.listFiles()) {
               if (subfile.isDirectory()) {
                  FileUtils.deleteDirectory(subfile);
               } else {
                  subfile.delete();
               }
            }
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }
   }
}
