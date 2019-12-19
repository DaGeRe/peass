package de.peran.analysis.helper.all;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.properties.ChangeProperties;
import de.peass.analysis.properties.ChangeProperty;
import de.peass.analysis.properties.PropertyProcessor;
import de.peass.analysis.properties.VersionChangeProperties;
import de.peass.dependency.persistence.Dependencies;
import de.peass.dependencyprocessors.VersionComparator;
import de.peass.statistics.DependencyStatisticAnalyzer;
import de.peran.FolderSearcher;
import de.peran.analysis.helper.AnalysisUtil;

public class GetAllClusterableStatistics {
   private static final Logger LOG = LogManager.getLogger(GetAllClusterableStatistics.class);
   
   public static void main(final String[] args) throws JAXBException, JsonParseException, JsonMappingException, IOException {
//      File dependencyFolder = new File("/home/reichelt/daten/diss/ergebnisse/views/v15_mitEnums/data");
//      File dataFolder = new File("/home/reichelt/daten/diss/ergebnisse/normaltest/v23_all");

      try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File("results/props_clusterable.csv")))) {
         for (final String project : new String[] {"commons-compress",  "commons-csv",  "commons-dbcp", "commons-fileupload", "commons-io", "commons-imaging", "commons-text"}) {
            LOG.info("Analyzing: {}", project);
            final File dependencyFile = new File(CleanAll.defaultDependencyFolder, "deps_" + project + ".xml");
            final Dependencies dependencies = DependencyStatisticAnalyzer.readVersions(dependencyFile);
            VersionComparator.setDependencies(dependencies);
            AnalysisUtil.setProjectName(project);
//            File changeFile = new File("results/" + project + "/clean.json");
//            File projectFolder = new File("../../projekte/" + project);
            final File resultFile = new File(CleanAll.defaultDataFolder, "results" + File.separator + project + File.separator + "properties.json");

            final VersionChangeProperties versionProperties = FolderSearcher.MAPPER.readValue(resultFile, VersionChangeProperties.class);

            versionProperties.executeProcessor(new PropertyProcessor() {

               @Override
               public void process(final String version, final String testcase, final ChangeProperty change, final ChangeProperties changeProperties) {
                  try {
                     writer.write(change.getChangePercent() + ";" + change.getAffectedLines() + ";" + (change.getCalls() - change.getCallsOld()) + "\n");
                  } catch (final IOException e) {
                     e.printStackTrace();
                  }

               }
            });
            writer.flush();
         }

      }
   }
}
