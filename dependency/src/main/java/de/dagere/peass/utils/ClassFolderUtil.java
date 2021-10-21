package de.dagere.peass.utils;

import java.util.LinkedList;
import java.util.List;

public class ClassFolderUtil {
   private static final List<String> pathes = new LinkedList<>();
   
   static {
      String[] potentialClassFolders = new String[] { "src/main/java/", "src/test/java/", "src/test/", "src/java/", "src/androidTest/java/" };
      for (String folder : potentialClassFolders) {
         pathes.add(folder);
      }
   }
   
   public static List<String> getPathes() {
      return pathes;
   }
   
}
