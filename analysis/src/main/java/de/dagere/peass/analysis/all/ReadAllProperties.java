package de.dagere.peass.analysis.all;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.ReadProperties;
import de.dagere.peass.analysis.properties.PropertyReader;
import de.dagere.peass.dependency.ResultsFolders;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.vcs.GitUtils;
import de.peran.FolderSearcher;

/**
 * The goal was to derive patterns from simple properties. This did not work out, therefore this class will be removed in the future
 * @author reichelt
 *
 */
@Deprecated
public class ReadAllProperties {
   public static final boolean readAll = System.getenv("read_all") != null ? Boolean.parseBoolean(System.getenv("read_all")) : false;

   public static void main(final String[] args) throws JAXBException, JsonParseException, JsonMappingException, JsonGenerationException, IOException, InterruptedException {
      final RepoFolders folders = new RepoFolders();
      
      final ExecutorService service = Executors.newFixedThreadPool(9);

      for (final String project : new String[] { "commons-compress", "commons-csv", "commons-dbcp", "commons-fileupload", "commons-jcs",
            "commons-imaging", "commons-io", "commons-numbers", "commons-pool", "commons-text" }) {

         final Runnable analyze = new Runnable() {
            @Override
            public void run() {
               try {
                  getProperties(folders, project);
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

   static void getProperties(final RepoFolders folders, final String project)
         throws JAXBException, IOException, JsonParseException, JsonMappingException, JsonGenerationException {
//      final File dependencyFile = new File(dependencyFolder, "deps_" + project + ".json");
//      final Dependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
//      VersionComparator.setDependencies(dependencies);
//      AnalysisUtil.setProjectName(resultsFolder, project);
      final File viewFolder = new File(folders.getAllViewFolder(), "views_" + project);

      final File executionFile = new File(folders.getDependencyFolder(), "execute_" + project + ".json");
      final ExecutionData changedTests = FolderSearcher.MAPPER.readValue(executionFile, ExecutionData.class);

      final File projectFolder = new File("../../projekte/" + project);
      if (!projectFolder.exists()) {
         GitUtils.downloadProject(changedTests.getUrl(), projectFolder);
      }
      
      if (!readAll) {
         final File changeFile = new File(folders.getResultsFolder(), project + File.separator + project + ".json");
         if (changeFile.exists()) {
            final File resultFile = new File(folders.getPropertiesFolder(), project + File.separator + project + ".json");
            if (!resultFile.getParentFile().exists()) {
               resultFile.getParentFile().mkdir();
            }
            final ReadProperties reader = new ReadProperties(resultFile);
            reader.readChangeProperties(changeFile, projectFolder, viewFolder, changedTests);
         } else {
            System.err.println("Error: " + changeFile.getAbsolutePath() + " does not exist");
         }
      } else {
         ResultsFolders resultsFolders = new ResultsFolders(folders.getPropertiesFolder().getParentFile(), project);
         new PropertyReader(resultsFolders, projectFolder, changedTests).readAllTestsProperties();
      }
   }
}
