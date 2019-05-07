package de.peass.analysis.all;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import de.peass.analysis.changes.ChangeReader;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.analysis.properties.VersionChangeProperties;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.statistics.DependencyStatisticAnalyzer;
import de.peass.utils.Constants;
import de.peran.FolderSearcher;
import de.peran.ReadProperties;
import de.peran.analysis.helper.AnalysisUtil;
import de.peran.analysis.helper.all.CleanAll;

public class GetAllChanges {
   public static void main(final String[] args) throws JAXBException {
      final RepoFolders folders = new RepoFolders(args);
      
      // for (final String project : new String[] { "commons-compress", "commons-csv", "commons-dbcp", "commons-fileupload", "commons-jcs",
      // "commons-imaging", "commons-io", "commons-numbers", "commons-pool", "commons-text" }) {
      for (final String project : new String[] { "commons-fileupload" }) {
         final File dependencyFile = new File(folders.getDependencyFolder(), "deps_" + project + ".json");
         if (dependencyFile.exists()) {
            final Dependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
            VersionComparator.setDependencies(dependencies);
            final File projectFolder = new File(folders.getDataFolder(), project);
            if (projectFolder.exists()) {
               File cleanFolder = new File(projectFolder, "clean");
               if (!cleanFolder.exists()) {
                  cleanFolder = projectFolder;
               }
               final ChangeReader reader = new ChangeReader(folders.getResultsFolder(), project);
               final ProjectChanges changes = reader.readFile(cleanFolder);
               if (changes != null) {
                  final File allPropertyFile = new File(folders.getPropertiesFolder(), project + File.separator + "properties_alltests.json");
                  if (allPropertyFile.exists()) {
                     final VersionChangeProperties properties = ReadProperties.readVersionProperties(changes, allPropertyFile);
                     try {
                        // final ProjectChanges oldKnowledge = ProjectChanges.getOldChanges();
                        // ProjectChanges.mergeKnowledge(oldKnowledge, knowledge);
                        // ReadProperties.writeOnlySource(properties, oldKnowledge);
                        FolderSearcher.MAPPER.writeValue(new File(AnalysisUtil.getProjectResultFolder(), "properties.json"), properties);
                     } catch (final IOException e) {
                        e.printStackTrace();
                     }
                  }
               }
            } else {
               System.out.println("Folder does not exist: " + projectFolder);
            }
            // ProjectChanges.reset();
         } else {
            System.out.println("Dependencyfile does not exist: " + dependencyFile);
         }
      }
   }
}
