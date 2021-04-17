package de.peass;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.config.DependencyReaderConfigMixin;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.statistics.DependencyStatisticAnalyzer;
import de.peass.statistics.DependencyStatistics;
import de.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

/**
 * Reads multiple dependency files and prints its statistics.
 * 
 * @author reichelt
 *
 */
@Command(description = "Reads the statistics of dependency files", name = "readDependencyStatistics")
public class DependencyStatisticSummarizer implements Callable<Void> {

   @Mixin
   private DependencyReaderConfigMixin config;

   public static void main(final String[] args) {
      try {
         final CommandLine commandLine = new CommandLine(new DependencyStatisticSummarizer());
         commandLine.execute(args);
      } catch (final Throwable t) {
         t.printStackTrace();
      }
   }

   @Override
   public Void call() throws Exception {
      final File folder = config.getResultBaseFolder();

      getExtendedTable(folder);

      System.out.println("====");
      getSimpleTable(folder);
      return null;
   }

   private void getSimpleTable(final File folder) throws IOException, JsonParseException, JsonMappingException, JAXBException {
      System.out.println("Project & Versions & Tests & SIC & TIC\\");
      for (final File xmlFile : FileUtils.listFiles(folder, new WildcardFileFilter("deps_*.xml"), TrueFileFilter.INSTANCE)) {
         final String projektName = xmlFile.getName().replace("deps_", "").replace(".xml", "");
         final File executeFile = new File(xmlFile.getParentFile(), "views_" + projektName + "/execute" + projektName + ".json");

         if (xmlFile.exists() && executeFile.exists()) {
            final ExecutionData changedTests = Constants.OBJECTMAPPER.readValue(executeFile, ExecutionData.class);

            final DependencyStatistics statistics = DependencyStatisticAnalyzer.getChangeStatistics(xmlFile, changedTests);

            final double percent = 10000d * statistics.getChangedTraceTests() / statistics.getOverallRunTests();
            System.out.println(percent);
            System.out.println(projektName + " & " + statistics.getSize() + " & " + statistics.getOverallRunTests() + " & " + statistics.getPruningRunTests() + " & "
                  + statistics.getChangedTraceTests() + " & " + Math.round(percent) / 100d + " %\\");

         }
      }
   }

   private void getExtendedTable(final File folder) throws IOException, JsonParseException, JsonMappingException, JAXBException {
      System.out.println("Project;Versions;Normal-Tests;SIC;TIC; Tests once changed; Tests multiple times changed");
      for (final File xmlFile : FileUtils.listFiles(folder, new WildcardFileFilter("deps_*.xml"), TrueFileFilter.INSTANCE)) {
         final String projektName = xmlFile.getName().replace("deps_", "").replace(".xml", "");
         final File executeFile = new File(xmlFile.getParentFile(), "views_" + projektName + "/execute" + projektName + ".json");

         if (xmlFile.exists() && executeFile.exists()) {
            final ExecutionData changedTests = Constants.OBJECTMAPPER.readValue(executeFile, ExecutionData.class);

            final DependencyStatistics statistics = DependencyStatisticAnalyzer.getChangeStatistics(xmlFile, changedTests);

            System.out.println(projektName + ";" + statistics.getSize() + ";" + statistics.getOverallRunTests() + ";" + statistics.getPruningRunTests() + ";"
                  + statistics.getChangedTraceTests() + ";"
                  + statistics.getOnceChangedTests().size() + ";" + statistics.getMultipleChangedTest().size());
         }
      }
   }
}
