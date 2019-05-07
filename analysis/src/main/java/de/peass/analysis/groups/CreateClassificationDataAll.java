package de.peass.analysis.groups;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.changes.ProjectChanges;
import de.peass.utils.Constants;
import de.peran.FolderSearcher;

public class CreateClassificationDataAll {

   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      if (System.getenv(Constants.PEASS_REPOS) != null) {
         final String repoFolder = System.getenv(Constants.PEASS_REPOS);

         final File changeFolder = new File(repoFolder, "measurementdata/results/");
         final File propertyRepo = new File(repoFolder, "properties");

         final File classificationFolder = new File(propertyRepo, "classification");
         for (final File projectFile : changeFolder.listFiles()) {
            final String project = projectFile.getName();
            final File changeFile = new File(projectFile, project + ".json");
            final File goalFile = new File(classificationFolder, project + ".json");
            if (changeFile.exists()) {
               final ProjectChanges changes = FolderSearcher.MAPPER.readValue(changeFile, ProjectChanges.class);
               CreateClassificationData.createClassificationData(changes, goalFile, project);
            } else {
               System.out.println("No Change File: " + project);
            }
         }
      } else {
         System.out.println("Error: PEASS_REPOS not defined");
      }
   }
}
