package de.peass.analysis.all;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.persistence.ExecutionData;
import de.peass.vcs.GitUtils;
import de.peran.FolderSearcher;
import de.peran.ReadProperties;

public class ReadAllProperties {
   public static final boolean readAll = System.getenv("read_all") != null ? Boolean.parseBoolean(System.getenv("read_all")) : false;

   public static void main(final String[] args) throws JAXBException, JsonParseException, JsonMappingException, JsonGenerationException, IOException, InterruptedException {
      final File repos = args.length > 0 ? new File(args[0]) : new File("/home/reichelt/daten3/diss/repos/");
      final File dependencyFolder = new File(repos, "dependencies-final");
      final File resultsFolder = new File(repos, "measurementdata/results");
      final File allViewFolder = new File(repos, "views-final");
      final File propertyFolder = new File(repos, "properties/properties/");
      resultsFolder.mkdirs();

      final ExecutorService service = Executors.newFixedThreadPool(9);

      for (final String project : new String[] { "commons-compress", "commons-csv", "commons-dbcp", "commons-fileupload", "commons-jcs",
            "commons-imaging", "commons-io", "commons-numbers", "commons-pool", "commons-text" }) {

         final Runnable analyze = new Runnable() {
            @Override
            public void run() {
               try {
                  getProperties(dependencyFolder, resultsFolder, allViewFolder, propertyFolder, project);
               } catch (final Throwable e) {
                  e.printStackTrace();
               }
            }
         };
         service.submit(analyze);
         Thread.sleep(5);
      }
      service.shutdown();
      try {
         final boolean success = service.awaitTermination(Long.MAX_VALUE, TimeUnit.HOURS);
         System.out.println("Success: " + success);
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }
   }

   static void getProperties(final File dependencyFolder, final File resultsFolder, final File allViewFolder, final File propertyFolder, final String project)
         throws JAXBException, IOException, JsonParseException, JsonMappingException, JsonGenerationException {
//      final File dependencyFile = new File(dependencyFolder, "deps_" + project + ".json");
//      final Dependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
//      VersionComparator.setDependencies(dependencies);
//      AnalysisUtil.setProjectName(resultsFolder, project);
      final File viewFolder = new File(allViewFolder, "views_" + project);

      final File executionFile = new File(dependencyFolder, "execute_" + project + ".json");
      final ExecutionData changedTests = FolderSearcher.MAPPER.readValue(executionFile, ExecutionData.class);

      final File projectFolder = new File("../../projekte/" + project);
      if (!projectFolder.exists()) {
         GitUtils.downloadProject(changedTests.getUrl(), projectFolder);
      }
      
      if (!readAll) {
         final File changeFile = new File(resultsFolder, project + File.separator + project + ".json");
         if (changeFile.exists()) {
            final File resultFile = new File(propertyFolder, project + File.separator + project + ".json");
            if (!resultFile.getParentFile().exists()) {
               resultFile.getParentFile().mkdir();
            }
            ReadProperties.readChangeProperties(changeFile, projectFolder, resultFile, viewFolder, changedTests);
         } else {
            System.err.println("Error: " + changeFile.getAbsolutePath() + " does not exist");
         }
      } else {
         final File resultFile = new File(propertyFolder, project + File.separator + "properties_alltests.json");
         ReadProperties.readAllTestsProperties(projectFolder, resultFile, viewFolder, changedTests);
      }
   }
}
