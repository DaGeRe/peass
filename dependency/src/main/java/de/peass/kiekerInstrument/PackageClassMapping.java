package de.peass.kiekerInstrument;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.peass.dependency.analysis.CalledMethodLoader;

public class PackageClassMapping {
   private static final Map<String, List<String>> loadedClasses = new HashMap();
   
   public static void clear() {
      loadedClasses.clear();
   }
   
   

   public List<String> getClasses(String packageName){
      List<String> result = loadedClasses.get(packageName);
      if (result == null) {
//         CalledMethodLoader
      }
      return result;
   }
   
}

