package de.dagere.peass.measurement.analysis;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.utils.Constants;

public class VersionSorter {
   public static ExecutionData executionData;

   public static void getVersionOrder(final File dependencyFile, final File executionFile, final File... additionalDependencyFiles)
         throws IOException, JsonParseException, JsonMappingException {
      Dependencies dependencies = null;
      executionData = null;
      if (dependencyFile != null) {
         dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
         VersionComparator.setDependencies(dependencies);
      }
      if (executionFile != null) {
         executionData = Constants.OBJECTMAPPER.readValue(executionFile, ExecutionData.class);
         dependencies = new Dependencies(executionData);
         VersionComparator.setDependencies(dependencies);
      }
      if (dependencies == null) {
         for (final File dependencytest : additionalDependencyFiles) {
            dependencies = Constants.OBJECTMAPPER.readValue(dependencytest, Dependencies.class);
            VersionComparator.setDependencies(dependencies);
         }
      }
      // if (executionData == null && dependencies == null) {
      // throw new RuntimeException("Dependencyfile and executionfile not readable - one needs to be defined and valid!");
      // }
   }
}
