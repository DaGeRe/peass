package de.peass.analysis.all;

import java.io.File;

import de.peass.utils.Constants;
import de.peran.analysis.helper.all.CleanAll;

public class RepoFolders {
   
   private final File DEFAULT_REPO = new File("/home/reichelt/daten3/diss/repos/");
   
   private final File dependencyFolder;
   private final File dataFolder;
   private final File propertiesFolder;
   private final File resultsFolder;
   private final File allViewFolder;

   public RepoFolders(String[] args) {
      final File repoFolder;
      if (System.getenv(Constants.PEASS_REPOS) != null) {
         final String repofolderName = System.getenv(Constants.PEASS_REPOS);
         repoFolder = new File(repofolderName);
      } else {
         repoFolder = DEFAULT_REPO;
      }
      dependencyFolder = new File(repoFolder, "dependencies-final");
      dataFolder = new File(repoFolder, "measurementdata" + File.separator + "cleanData");
      propertiesFolder = new File(repoFolder, "properties" + File.separator + "properties");
      resultsFolder = new File(repoFolder, "measurementdata" + File.separator + "results");
      allViewFolder = new File(repoFolder, "views-final");
      
      resultsFolder.mkdirs();
   }

   public File getDependencyFolder() {
      return dependencyFolder;
   }

   public File getDataFolder() {
      return dataFolder;
   }

   public File getPropertiesFolder() {
      return propertiesFolder;
   }

   public File getResultsFolder() {
      return resultsFolder;
   }

   public File getAllViewFolder() {
      return allViewFolder;
   }
}
