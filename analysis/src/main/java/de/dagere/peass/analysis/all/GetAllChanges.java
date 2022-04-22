package de.dagere.peass.analysis.all;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.ReadProperties;
import de.dagere.peass.analysis.changes.ChangeReader;
import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.analysis.properties.VersionChangeProperties;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;

public class GetAllChanges {
   public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException {
      final RepoFolders folders = new RepoFolders();
      
      // for (final String project : new String[] { "commons-compress", "commons-csv", "commons-dbcp", "commons-fileupload", "commons-jcs",
      // "commons-imaging", "commons-io", "commons-numbers", "commons-pool", "commons-text" }) {
      for (final String project : new String[] { "commons-fileupload" }) {
         final File dependencyFile = new File(folders.getDependencyFolder(), ResultsFolders.STATIC_SELECTION_PREFIX + project + ".json");
         if (dependencyFile.exists()) {
            final StaticTestSelection dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, StaticTestSelection.class);
            getChangesForProject(folders, project, dependencies);
            // ProjectChanges.reset();
         } else {
            System.out.println("Dependencyfile does not exist: " + dependencyFile);
         }
      }
   }

   public static void getChangesForProject(final RepoFolders folders, final String project, final StaticTestSelection dependencies) throws FileNotFoundException {
      final File projectFolder = new File(folders.getCleanDataFolder(), project);
      if (projectFolder.exists()) {
         File cleanFolder = new File(projectFolder, "clean");
         if (!cleanFolder.exists()) {
            cleanFolder = projectFolder;
         }
         getChangesForMeasurementfolder(folders, project, cleanFolder, dependencies);
         File reexecuteFolder = new File(folders.getReexecuteFolder(), project);
         if (reexecuteFolder.exists()) {
            for (File iterationFolder : reexecuteFolder.listFiles()) {
               getChangesForMeasurementfolder(folders, project, iterationFolder, dependencies);
            }
         }
      } else {
         System.out.println("Folder does not exist: " + projectFolder);
      }
   }

   public static void getChangesForMeasurementfolder(final RepoFolders folders, final String project, final File cleanFolder, final StaticTestSelection dependencies) throws FileNotFoundException {
      ResultsFolders resultsFolders = new ResultsFolders(folders.getProjectStatisticsFolder(project), project);
      final ChangeReader reader = new ChangeReader(resultsFolders, dependencies);
      final ProjectChanges changes = reader.readFile(cleanFolder);
      if (changes != null) {
         final File allPropertyFile = new File(folders.getPropertiesFolder(), project + File.separator + "properties_alltests.json");
         if (allPropertyFile.exists()) {
            final VersionChangeProperties properties = ReadProperties.readVersionProperties(changes, allPropertyFile);
            try {
               // final ProjectChanges oldKnowledge = ProjectChanges.getOldChanges();
               // ProjectChanges.mergeKnowledge(oldKnowledge, knowledge);
               // ReadProperties.writeOnlySource(properties, oldKnowledge);
               Constants.OBJECTMAPPER.writeValue(folders.getProjectPropertyFile(project), properties);
            } catch (final IOException e) {
               e.printStackTrace();
            }
         }
      }
   }
}
