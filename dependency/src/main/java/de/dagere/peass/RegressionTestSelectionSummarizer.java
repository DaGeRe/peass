package de.dagere.peass;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.config.parameters.TestSelectionConfigMixin;
import de.dagere.peass.dependency.persistence.ExecutionData;
import de.dagere.peass.dependency.statistics.DependencyStatisticAnalyzer;
import de.dagere.peass.dependency.statistics.DependencyStatistics;
import de.dagere.peass.folders.ResultsFolders;
import de.dagere.peass.utils.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

/**
 * Reads multiple dependency files and prints its statistics.
 * 
 * @author reichelt
 *
 */
@Command(description = "Reads the statistics of static selection files", name = "readSelectionStatistics")
public class RegressionTestSelectionSummarizer implements Callable<Void> {

   @Mixin
   private TestSelectionConfigMixin config;

   public static void main(final String[] args) {
      try {
         final CommandLine commandLine = new CommandLine(new RegressionTestSelectionSummarizer());
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

   private void getSimpleTable(final File folder) throws IOException, JsonParseException, JsonMappingException {
      System.out.println("Project & Versions & Tests & SIC & TIC\\");
      for (final File xmlFile : FileUtils.listFiles(folder, new WildcardFileFilter(ResultsFolders.STATIC_SELECTION_PREFIX +"*.json"), TrueFileFilter.INSTANCE)) {
         final String projektName = xmlFile.getName().replace(ResultsFolders.STATIC_SELECTION_PREFIX, "").replace(".xml", "");
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

   private void getExtendedTable(final File folder) throws IOException, JsonParseException, JsonMappingException {
      System.out.println("Project;Versions;Normal-Tests;SIC;TIC; Tests once changed; Tests multiple times changed");
      for (final File xmlFile : FileUtils.listFiles(folder, new WildcardFileFilter(ResultsFolders.STATIC_SELECTION_PREFIX +"*.json"), TrueFileFilter.INSTANCE)) {
         final String projektName = xmlFile.getName().replace(ResultsFolders.STATIC_SELECTION_PREFIX, "").replace(".xml", "");
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
