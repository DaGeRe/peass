package de.dagere.peass.analysis.all;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.utils.Constants;
import de.peran.FolderSearcher;

public class RepoFolders {

   private final File DEFAULT_REPO = new File("/home/reichelt/daten3/diss/repos/");

   private final File dependencyFolder;
   private final File dataFolder;
   private final File reexecuteFolder;

   private final File propertiesRepo;
   private final File classificationFolder;
   private final File resultsFolder;
   private final File allViewFolder;
   private final File measurementdata;

   public RepoFolders() {
      final File repoFolder;
      if (System.getenv(Constants.PEASS_REPOS) != null) {
         final String repofolderName = System.getenv(Constants.PEASS_REPOS);
         repoFolder = new File(repofolderName);
      } else {
         repoFolder = DEFAULT_REPO;
      }
      measurementdata = new File(repoFolder, "measurementdata");
      if (!measurementdata.exists()) {
         measurementdata.mkdirs();
      }
      dependencyFolder = new File(repoFolder, "dependencies-final");
      dataFolder = new File(measurementdata, "confidentClean");
      reexecuteFolder = new File(measurementdata, "reexecute");
      propertiesRepo = new File(repoFolder, "properties");
      classificationFolder = new File(propertiesRepo, "classification");
      resultsFolder = new File(measurementdata, "results");

      allViewFolder = new File(repoFolder, "views-final");

      resultsFolder.mkdirs();
   }

   public File getDependencyFolder() {
      return dependencyFolder;
   }

   public File getCleanDataFolder() {
      return dataFolder;
   }

   public File getValidationDataFolder(final String project) {
      return new File(dataFolder.getParentFile(), "validation" + File.separator + "clean" + File.separator + project);
   }

   public File getReexecuteFolder() {
      return reexecuteFolder;
   }

   public File getPropertiesFolder() {
      return propertiesRepo;
   }

   public File getResultsFolder() {
      return resultsFolder;
   }

   public File getAllViewFolder() {
      return allViewFolder;
   }

   public File getClassificationFolder() {
      return classificationFolder;
   }

   public File getProjectPropertyFile(final String project) {
      return new File(propertiesRepo, "properties" + File.separator + project + File.separator + project + ".json");
   }

   public File getProjectStatisticsFolder(final String project) {
      final File statisticsFolder = new File(resultsFolder, project + File.separator + "statistics");
      if (!statisticsFolder.exists()) {
         statisticsFolder.mkdirs();
      }
      return statisticsFolder;
   }

   public File getDependencyFile(final String project) {
      return new File(dependencyFolder, "deps_" + project + ".json");
   }

   public ExecutionData getExecutionData(final String project) throws JsonParseException, JsonMappingException, IOException {
      final File executionFile = new File(dependencyFolder, "execute_" + project + ".json");
      final ExecutionData changedTests = FolderSearcher.MAPPER.readValue(executionFile, ExecutionData.class);
      return changedTests;
   }

   public File getChangeFile(final String project) {
      File candidate = new File(resultsFolder, project + File.separator + project + ".json");
      if (!candidate.exists()) {
         candidate = new File(resultsFolder, project + ".json");
      }
      return candidate;
   }

   public File getViewFolder(final String project) {
      return new File(allViewFolder, "views_" + project);
   }

   public File getRCAScriptFolder() {
      final File scriptFolder = new File(measurementdata, "rca" + File.separator + "scripts");
      if (!scriptFolder.exists()) {
         scriptFolder.mkdirs();
      }
      return scriptFolder;
   }

}
