package de.peran.analysis.helper;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.analysis.changes.Changes;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.utils.Constants;

public class CompareMeasurements {
   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      File changeFile1 = new File(args[0]);
      File changeFile2 = new File(args[1]);

      ProjectChanges changes1 = Constants.OBJECTMAPPER.readValue(changeFile1, ProjectChanges.class);
      ProjectChanges changes2 = Constants.OBJECTMAPPER.readValue(changeFile2, ProjectChanges.class);

      Set<String> allVersions = new LinkedHashSet<>();
      allVersions.addAll(changes1.getVersionChanges().keySet());
      allVersions.addAll(changes2.getVersionChanges().keySet());

      int notfound = 0;
      for (String version : allVersions) {
         System.out.println("Version: " + version);
         Changes version1changes = changes1.getVersion(version);
         Changes version2changes = changes2.getVersion(version);

         Set<String> changeClazzes = new HashSet<>();

         changeClazzes.addAll(version1changes.getTestcaseChanges().keySet());
         changeClazzes.addAll(version2changes.getTestcaseChanges().keySet());

         for (String changedClazz : changeClazzes) {
            notfound+=findMissing(changedClazz, version1changes.getTestcaseChanges(), version2changes.getTestcaseChanges());
            notfound+=findMissing(changedClazz, version2changes.getTestcaseChanges(), version1changes.getTestcaseChanges());
         }
      }
      System.out.println("Not found: " + notfound);
      // for (changes1.getVersionChanges().key)
   }

   private static int findMissing(final String changedClazz, final Map<String, List<Change>> testcaseChanges2, final Map<String, List<Change>> testcaseChanges) {
      int notfound = 0;
      List<Change> changes = testcaseChanges.get(changedClazz);
      if (changes != null) {
         for (Change change : changes) {
//            System.out.println("Searching: " + changedClazz);
            boolean found = false;
            List<Change> otherChanges = testcaseChanges2.get(changedClazz);
            if (otherChanges != null) {
               for (Change change2 : otherChanges) {
                  if (change.getDiff().equals(change2.getDiff())) {
                     found = true;
//                     System.out.println("Found: " + change.getDiff());
                  }
               }
            }

            if (!found) {
               System.out.println("Not found: " + change.getDiff());
               notfound++;
            }
         }
      }
      return notfound;
   }
}
