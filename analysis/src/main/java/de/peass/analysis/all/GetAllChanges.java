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
      
      final File dependencyFolder;final File dataFolder;
      final File propertiesFolder;
      final File resultsFolder;
      if (System.getenv(Constants.PEASS_REPOS) != null) {
         final String repofolder = System.getenv(Constants.PEASS_REPOS);
         final File folder = new File(repofolder);
         dependencyFolder = new File(folder, "dependencies-final");
         dataFolder = new File(folder, "measurementdata" + File.separator + "cleanData");
         propertiesFolder = new File(folder, "properties" + File.separator + "properties");
         resultsFolder = new File(folder, "measurementdata" + File.separator + "results");
      }else {
         dependencyFolder = new File(CleanAll.defaultDependencyFolder);
         dataFolder = new File(args.length > 0 ? args[0] : CleanAll.defaultDataFolder);
         propertiesFolder = new File("/home/reichelt/daten/diss/ergebnisse/properties/v2/results/");
         resultsFolder = args.length > 0 ? new File(args[0], "results") : new File("results");
      }
      
      resultsFolder.mkdirs();
//      for (final String project : new String[] { "commons-compress", "commons-csv", "commons-dbcp", "commons-fileupload", "commons-jcs",
//            "commons-imaging", "commons-io", "commons-numbers", "commons-pool", "commons-text" }) {
      for (final String project : new String[] { "commons-compress"}) {
         final File dependencyFile = new File(dependencyFolder, "deps_" + project + ".json");
         if (dependencyFile.exists()) {
            final Dependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
            VersionComparator.setDependencies(dependencies);
            final File projectFolder = new File(dataFolder, project);
            if (projectFolder.exists()) {
               File cleanFolder = new File(projectFolder, "clean");
               if (!cleanFolder.exists()) {
                  cleanFolder = projectFolder;
               }
               final ChangeReader reader = new ChangeReader(resultsFolder, project);
               final ProjectChanges changes = reader.readFile(cleanFolder);
               if (changes != null) {
                  final File allPropertyFile = new File(propertiesFolder, project + File.separator + "properties_alltests.json");
                  if (allPropertyFile.exists()) {
                     final VersionChangeProperties properties = ReadProperties.readVersionProperties(changes, allPropertyFile);
                     try {
//                        final ProjectChanges oldKnowledge = ProjectChanges.getOldChanges();
//                        ProjectChanges.mergeKnowledge(oldKnowledge, knowledge);
//                        ReadProperties.writeOnlySource(properties, oldKnowledge);
                        FolderSearcher.MAPPER.writeValue(new File(AnalysisUtil.getProjectResultFolder(), "properties.json"), properties);
                     } catch (final IOException e) {
                        e.printStackTrace();
                     }
                  }
               }
            } else {
               System.out.println("Folder does not exist: " + projectFolder);
            }
//            ProjectChanges.reset();
         } else {
            System.out.println("Dependencyfile does not exist: " + dependencyFile);
         }
      }
   }
}
