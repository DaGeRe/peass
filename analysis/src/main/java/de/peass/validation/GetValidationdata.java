package de.peass.validation;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.changes.Change;
import de.peass.analysis.changes.Changes;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.validation.data.ProjectValidation;
import de.peass.validation.data.Validation;
import de.peass.validation.data.ValidationChange;
import de.peass.vcs.GitUtils;
import de.peran.FolderSearcher;

/**
 * Reads validation data from normal measurement results. This assumes that all experiments have been executed.
 */
public class GetValidationdata {

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final File repoFolder = new File(args[0]);

      final File propertyRepo = new File(repoFolder, "properties");
      final File classificationFolder = new File(propertyRepo, "classification");
      final File changeFolder = new File(repoFolder, "measurementdata/results/");
      final File dependencyFolder = new File(repoFolder, "dependencies-final/");
      final File commitFolder = new File(repoFolder, "properties/validation/");

      getValidation(changeFolder, dependencyFolder, commitFolder, new File("validation.json"));
   }

   public static void getValidation(final File changeFolder, final File dependencyFolder, final File commitFolder, File validationFile)
         throws IOException, JsonParseException, JsonMappingException, JsonGenerationException {
      final File commitFile = new File(commitFolder, "performance_commits.json");

      final Map<String, Map<String, String>> data = FolderSearcher.MAPPER.readValue(commitFile, Map.class);
      data.remove("commons-compress");
      data.remove("commons-jcs");

      Validation old;
      if (validationFile.exists()) {
         old = FolderSearcher.MAPPER.readValue(validationFile, Validation.class);
      } else {
         old = null;
      }

      final Validation validation = new Validation();

      for (final Map.Entry<String, Map<String, String>> project : data.entrySet()) {
         Validator validator = new Validator(dependencyFolder, changeFolder, project.getKey());
         ProjectValidation projectValidation = validator.validateProject(old, project.getValue());
         validation.getProjects().put(project.getKey(), projectValidation);
      }

      FolderSearcher.MAPPER.writeValue(validationFile, validation);
   }

}
