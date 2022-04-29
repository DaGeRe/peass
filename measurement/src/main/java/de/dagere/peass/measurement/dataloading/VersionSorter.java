package de.dagere.peass.measurement.dataloading;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.persistence.SelectedTests;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.utils.Constants;

public class VersionSorter {
   
   public static SelectedTests getSelectedTests(final File dependencyFile, final File executionFile, final File... additionalDependencyFiles)
         throws IOException, JsonParseException, JsonMappingException {
      StaticTestSelection dependencies = null;
      ExecutionData executionData = null;
      if (dependencyFile != null) {
         dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, StaticTestSelection.class);
         return dependencies;
      }
      if (executionFile != null) {
         executionData = Constants.OBJECTMAPPER.readValue(executionFile, ExecutionData.class);
         return executionData;
      }
      if (dependencies == null) {
         for (final File dependencytest : additionalDependencyFiles) {
            dependencies = Constants.OBJECTMAPPER.readValue(dependencytest, StaticTestSelection.class);
            return dependencies;
         }
      }
      throw new RuntimeException("No dependencyfile provided");
   }
}
