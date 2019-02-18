package de.peass.analysis.groups;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.peass.dependency.analysis.data.ChangedEntity;

public class Classification {
   private Map<String, VersionClass> versions = new LinkedHashMap<>();

   public Map<String, VersionClass> getVersions() {
      return versions;
   }

   public void setVersions(final Map<String, VersionClass> versions) {
      this.versions = versions;
   }

   public void addChange(final String version, final ChangedEntity test, final Set<String> guessedTypes, final String direction) {
      VersionClass versionClazz = versions.get(version);
      if (versionClazz == null) {
         versionClazz = new VersionClass();
         versions.put(version, versionClazz);
      }
      versionClazz.addTestcase(test, guessedTypes, direction);
      
   }
   
   public void merge(final Classification other) {
      for (final Map.Entry<String, VersionClass> version : other.getVersions().entrySet()) {
         final VersionClass mergedVersion = getVersions().get(version.getKey());
         if (mergedVersion != null) {
            for (final Entry<ChangedEntity, TestcaseClass> testcase : version.getValue().getTestcases().entrySet()) {
               final TestcaseClass data = mergedVersion.getTestcases().get(testcase.getKey());
               if (data != null) {
                  data.merge(testcase.getValue());
                  
               } else {
                  mergedVersion.getTestcases().put(testcase.getKey(), testcase.getValue());
               }
            }
         } else {
            getVersions().put(version.getKey(), version.getValue());
         }
      }
   }
}
