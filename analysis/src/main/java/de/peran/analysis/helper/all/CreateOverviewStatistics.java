package de.peran.analysis.helper.all;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.xml.bind.JAXBException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.analysis.changes.ProjectChanges;
import de.peass.analysis.properties.ChangeProperties;
import de.peass.analysis.properties.ChangeProperty;
import de.peass.analysis.properties.VersionChangeProperties;
import de.peass.dependency.analysis.data.TestSet;
import de.peass.dependency.persistence.ExecutionData;
import de.peass.vcs.GitUtils;
import de.peran.FolderSearcher;
import de.peran.measurement.analysis.changes.processors.PropertyProcessor;

public class CreateOverviewStatistics {
   static class ProjectStatistics implements PropertyProcessor {
      DescriptiveStatistics affectedLines = new DescriptiveStatistics();
      DescriptiveStatistics calls = new DescriptiveStatistics();
      DescriptiveStatistics changes = new DescriptiveStatistics();

      @Override
      public void process(String version, String testcase, ChangeProperty change, ChangeProperties changeProperties) {
         affectedLines.addValue(change.getAffectedLines());
         calls.addValue((change.getCalls() - change.getCallsOld()) / 2l);
         changes.addValue(-change.getChangePercent());
      }
   }

   public static void main(String[] args) throws JAXBException, JsonParseException, JsonMappingException, IOException {
      File dependencyFolder = new File(CleanAll.defaultDependencyFolder);
      File resultsFolder = new File("/home/reichelt/daten/diss/ergebnisse/normaltest/v26_symbolicComplete/results");
      File propertyFolder = new File("/home/reichelt/daten/diss/ergebnisse/properties/v1/results/");
      DescriptiveStatistics stats[] = new DescriptiveStatistics[9];
      for (int i = 0; i < 9; i++) {
         stats[i] = new DescriptiveStatistics();
      }
      for (String project : new String[] { "compress", "csv", "dbcp", "fileupload", "imaging", "io", "text" }) {
         System.out.print(project + " & ");
         int versions = GitUtils.getVersions(new File("../../projekte/commons-" + project));
         System.out.print(versions + " & ");
         stats[0].addValue(versions);
         File executionFile = new File(dependencyFolder, "views_commons-"+project + File.separator + "execute-commons-" + project + ".json");
         ExecutionData changedTests = FolderSearcher.MAPPER.readValue(executionFile, ExecutionData.class);
         System.out.print(changedTests.getVersions().size() + " & ");
         stats[1].addValue(changedTests.getVersions().size());
         int tests = 0;
         for (TestSet testSet : changedTests.getVersions().values()) {
            tests += testSet.getTests().size();
         }
         System.out.print(tests + " & ");
         stats[2].addValue(tests);

         int changes = 0, sourceChanges = 0, sourceTests = 0;
         File potentialChangeFolder = new File(resultsFolder, "commons-" + project);
         if (potentialChangeFolder.exists()) {
            File changefile = new File(potentialChangeFolder, "clean.json");
            ProjectChanges measuredChanges = FolderSearcher.MAPPER.readValue(changefile, ProjectChanges.class);
            changes = measuredChanges.getChangeCount();

            File changefileOnlysource = new File(potentialChangeFolder, "changes_onlysource.json");
            if (changefileOnlysource.exists()) {
               ProjectChanges measuredChangesOnlysource = FolderSearcher.MAPPER.readValue(changefileOnlysource, ProjectChanges.class);
               sourceChanges = measuredChangesOnlysource.getChangeCount();
            }
         }
         
         File allTestProps = new File(propertyFolder, "commons-" + project + "/properties_alltests.json");
         if (allTestProps.exists()) {
            VersionChangeProperties properties = FolderSearcher.MAPPER.readValue(allTestProps, VersionChangeProperties.class);
            sourceTests = properties.getSourceChanges();
         }

         System.out.print(sourceTests + " & ");
         stats[3].addValue(sourceTests);

         System.out.print(changes + " & ");
         stats[4].addValue(changes);

         System.out.print(sourceChanges + " & ");
         stats[5].addValue(sourceChanges);

         File resultFile = new File(potentialChangeFolder, "properties.json");

         VersionChangeProperties versionProperties = FolderSearcher.MAPPER.readValue(resultFile, VersionChangeProperties.class);

         ProjectStatistics projectStatistics = new ProjectStatistics();
         versionProperties.executeProcessor(projectStatistics);

         System.out.print(new DecimalFormat("##.##").format(projectStatistics.affectedLines.getMean()) + " & ");
         stats[6].addValue(projectStatistics.affectedLines.getMean());
         // System.out.print(" & " + projectStatistics.affectedLines.getStandardDeviation() / projectStatistics.affectedLines.getMean() + " & ");
         System.out.print(new DecimalFormat("##.##").format(projectStatistics.calls.getMean())+ " & ");
         stats[7].addValue(projectStatistics.calls.getMean());
         // System.out.print(" & " + projectStatistics.calls.getStandardDeviation() / projectStatistics.calls.getMean()+ " & ");
         System.out.print(new DecimalFormat("##.##").format(projectStatistics.changes.getMean()) + " \\% ");
         stats[8].addValue(projectStatistics.changes.getMean());
         // System.out.print(" & " + projectStatistics.changes.getStandardDeviation() / projectStatistics.changes.getMean());

         System.out.print(" \\\\");

         System.out.println();
      }
      System.out.println("\\hline");
      System.out.print(" & ");
      for (int i = 0; i < 9; i++) {
         System.out.print(new DecimalFormat("##.##").format(stats[i].getMean()) + " & ");
         
      }
      System.out.println();
      for (int i = 0; i < 9; i++) {
         System.out.print(stats[i].getSum() + " & ");
      }
      System.out.println();
   }
}
