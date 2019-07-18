package de.peass.analysis.groups;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.peass.analysis.changes.ChangeReader;
import de.peass.dependency.analysis.data.ChangedEntity;

public class Classification {
   private static final Logger LOG = LogManager.getLogger(Classification.class);

   private Map<String, VersionClass> versions = new LinkedHashMap<>();

   public Map<String, VersionClass> getVersions() {
      return versions;
   }

   public void setVersions(final Map<String, VersionClass> versions) {
      this.versions = versions;
   }

   @JsonIgnore
   public TestcaseClass addChange(final String version, final ChangedEntity test, final Set<String> guessedTypes, final String direction) {
      VersionClass versionClazz = versions.get(version);
      if (versionClazz == null) {
         versionClazz = new VersionClass();
         versions.put(version, versionClazz);
      }
      return versionClazz.addTestcase(test, guessedTypes, direction);
   }

   public void merge(final Classification other) {
      for (final Map.Entry<String, VersionClass> otherVersion : other.getVersions().entrySet()) {
         if (otherVersion.getKey().equals("3ac3b9c39c2637fd92ce89caf066e3058bb17d3c")) {
            System.out.println("here");
         }
         final VersionClass mergedVersion = versions.get(otherVersion.getKey());
         if (mergedVersion != null) {
            for (final Entry<ChangedEntity, TestcaseClass> testcase : otherVersion.getValue().getTestcases().entrySet()) {
               final TestcaseClass data = mergedVersion.getTestcases().get(testcase.getKey());
               if (data != null) {
                  if (data.getDirection() != null) {
                     data.merge(testcase.getValue());
                  } else {
                     LOG.error("Raw data contained null-direction: {}", testcase.getKey());
                  }
               } else {
                  LOG.error("Version only present in input-data: {} - {}", otherVersion.getKey(), testcase.getKey());
               }
            }
         } else {
            LOG.info("Input data contained version with change that was not measured: {}", otherVersion.getKey());
         }
      }
   }
   
   public void mergeAll(final Classification other) {
      for (final Map.Entry<String, VersionClass> otherVersion : other.getVersions().entrySet()) {
         final VersionClass mergedVersion = versions.get(otherVersion.getKey());
         if (mergedVersion != null) {
            for (final Entry<ChangedEntity, TestcaseClass> testcase : otherVersion.getValue().getTestcases().entrySet()) {
               final TestcaseClass data = mergedVersion.getTestcases().get(testcase.getKey());
               if (data != null) {
                  if (data.getDirection() != null) {
                     data.merge(testcase.getValue());
                  } else {
                     LOG.error("Raw data contained null-direction: {}", testcase.getKey());
                  }
               } else {
                  mergedVersion.getTestcases().put(testcase.getKey(), testcase.getValue());
               }
            }
         } else {
            versions.put(otherVersion.getKey(), otherVersion.getValue());
         }
      }
   }
}
