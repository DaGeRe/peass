package de.peran.analysis.helper.all;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.persistence.Dependencies;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.statistics.DependencyStatisticAnalyzer;
import de.peran.FolderSearcher;
import de.peran.ReadProperties;
import de.peran.analysis.helper.AnalysisUtil;

public class ReadAllProperties {
   public static final boolean readAll = System.getenv("read_all") != null ? Boolean.parseBoolean(System.getenv("read_all")) : false;

   public static void main(final String[] args) throws JAXBException, JsonParseException, JsonMappingException, JsonGenerationException, IOException {
      final File dependencyFolder = new File(args.length > 0 ? args[0] : CleanAll.defaultDependencyFolder);
      final File resultFolder = new File("/home/reichelt/daten/diss/ergebnisse/normaltest/v26_symbolicComplete", "results");
      // File resultFolder = new File("/home/reichelt/daten/diss/ergebnisse/normaltest/v27", "results");
      // for (String project : new String[] { "commons-compress", "commons-csv", "commons-dbcp", "commons-fileupload", "commons-imaging", "commons-io",
      // "commons-numbers" }) {
//      for (String project : new String[] { "commons-text" }) {
          for (final String project : CleanAll.allProjects) {

//         ProjectChanges.reset();
         final File dependencyFile = new File(dependencyFolder, "deps_" + project + ".xml");
         final Dependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
         VersionComparator.setDependencies(dependencies);
         AnalysisUtil.setProjectName(resultFolder, project);
         final File viewFolder = new File(dependencyFolder, "views_" + project);

         final File projectFolder = new File("../../projekte/" + project);

         final File executionFile = new File(viewFolder, "execute-" + project + ".json");
         final ExecutionData changedTests = FolderSearcher.MAPPER.readValue(executionFile, ExecutionData.class);

         if (!readAll) {
            final File changeFile = new File(resultFolder, project + "/clean.json");
            if (changeFile.exists()) {
               final File resultFile = new File(resultFolder, project + File.separator + "properties.json");
               ReadProperties.readChangeProperties(changeFile, projectFolder, resultFile, viewFolder, changedTests);
            }
         } else {
            final File resultFile = new File("results" + File.separator + project + File.separator + "properties_alltests.json");
            ReadProperties.readAllTestsProperties(projectFolder, resultFile, viewFolder, changedTests);
         }

      }
   }
}
