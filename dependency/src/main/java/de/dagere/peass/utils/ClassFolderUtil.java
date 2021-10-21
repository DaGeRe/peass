package de.dagere.peass.utils;

import java.util.HashSet;
import java.util.Set;

public class ClassFolderUtil {
   private static final Set<String> pathes = new HashSet<>();
   
   static {
      String[] potentialClassFolders = new String[] { "src/main/java/", "src/test/java/", "src/test/", "src/java/", "src/androidTest/java/" };
      for (String folder : potentialClassFolders) {
         pathes.add(folder);
      }
   }
   
   public static Set<String> getPathes() {
      return pathes;
   }
   
}
