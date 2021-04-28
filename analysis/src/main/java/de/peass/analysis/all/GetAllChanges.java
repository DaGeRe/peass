package de.peass.analysis.all;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.analysis.properties.VersionChangeProperties;
import de.dagere.peass.dependency.persistence.Dependencies;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.utils.Constants;
import de.peass.ReadProperties;
import de.peass.analysis.changes.ChangeReader;
import de.peass.analysis.changes.ProjectChanges;
import de.peran.FolderSearcher;

public class GetAllChanges {
   public static void main(final String[] args) throws JAXBException, JsonParseException, JsonMappingException, IOException {
      final RepoFolders folders = new RepoFolders();
      
      // for (final String project : new String[] { "commons-compress", "commons-csv", "commons-dbcp", "commons-fileupload", "commons-jcs",
      // "commons-imaging", "commons-io", "commons-numbers", "commons-pool", "commons-text" }) {
      for (final String project : new String[] { "commons-fileupload" }) {
         final File dependencyFile = new File(folders.getDependencyFolder(), "deps_" + project + ".json");
         if (dependencyFile.exists()) {
            final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
            VersionComparator.setDependencies(dependencies);
            getChangesForProject(folders, project);
            // ProjectChanges.reset();
         } else {
            System.out.println("Dependencyfile does not exist: " + dependencyFile);
         }
      }
   }

   public static void getChangesForProject(final RepoFolders folders, final String project) throws JAXBException, FileNotFoundException {
      final File projectFolder = new File(folders.getCleanDataFolder(), project);
      if (projectFolder.exists()) {
         File cleanFolder = new File(projectFolder, "clean");
         if (!cleanFolder.exists()) {
            cleanFolder = projectFolder;
         }
         getChangesForMeasurementfolder(folders, project, cleanFolder);
         File reexecuteFolder = new File(folders.getReexecuteFolder(), project);
         if (reexecuteFolder.exists()) {
            for (File iterationFolder : reexecuteFolder.listFiles()) {
               getChangesForMeasurementfolder(folders, project, iterationFolder);
            }
         }
      } else {
         System.out.println("Folder does not exist: " + projectFolder);
      }
   }

   public static void getChangesForMeasurementfolder(final RepoFolders folders, final String project, final File cleanFolder) throws JAXBException, FileNotFoundException {
      final ChangeReader reader = new ChangeReader(folders, project);
      reader.setType1error(0.02);
      reader.setType2error(0.02);
      final ProjectChanges changes = reader.readFile(cleanFolder);
      if (changes != null) {
         final File allPropertyFile = new File(folders.getPropertiesFolder(), project + File.separator + "properties_alltests.json");
         if (allPropertyFile.exists()) {
            final VersionChangeProperties properties = ReadProperties.readVersionProperties(changes, allPropertyFile);
            try {
               // final ProjectChanges oldKnowledge = ProjectChanges.getOldChanges();
               // ProjectChanges.mergeKnowledge(oldKnowledge, knowledge);
               // ReadProperties.writeOnlySource(properties, oldKnowledge);
               FolderSearcher.MAPPER.writeValue(folders.getProjectPropertyFile(project), properties);
            } catch (final IOException e) {
               e.printStackTrace();
            }
         }
      }
   }
}
