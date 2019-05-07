package de.peass.overviewTables;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

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
import de.peass.utils.Constants;
import de.peass.vcs.GitUtils;
import de.peran.FolderSearcher;
import de.peran.analysis.helper.all.CleanAll;
import de.peran.measurement.analysis.changes.processors.PropertyProcessor;

/**
 * Creates overview-table for ESEC-paper
 * @author reichelt
 *
 */
public class CreateOverviewStatistics {
   static class ProjectStatistics implements PropertyProcessor {
      DescriptiveStatistics affectedLines = new DescriptiveStatistics();
      DescriptiveStatistics calls = new DescriptiveStatistics();
      DescriptiveStatistics changes = new DescriptiveStatistics();

      @Override
      public void process(final String version, final String testcase, final ChangeProperty change, final ChangeProperties changeProperties) {
         affectedLines.addValue(change.getAffectedLines());
         calls.addValue((change.getCalls() - change.getCallsOld()) / 2l);
         if (!Double.isNaN(change.getChangePercent())) {
            changes.addValue(-change.getChangePercent());
         }
      }
   }

   public static void main(final String[] args) throws JAXBException, JsonParseException, JsonMappingException, IOException {
      File dependencyFolder;
      final File repos;
      if (System.getenv(Constants.PEASS_REPOS) != null) {
         final String repofolder = System.getenv(Constants.PEASS_REPOS);
         repos = new File(repofolder);
         dependencyFolder = new File(repos, "dependencies-final");
      } else {
         dependencyFolder = new File(CleanAll.defaultDependencyFolder);
         repos = dependencyFolder.getParentFile();
      }

      final File propertyFolder = new File(repos, "properties/properties");
      final File changeFolder = new File(repos, "measurementdata/results");
      final File projectsFolder = new File("../../projekte");

      final DescriptiveStatistics stats[] = new DescriptiveStatistics[9];
      for (int i = 0; i < 9; i++) {
         stats[i] = new DescriptiveStatistics();
      }
      // raus:
      // for (final String project : new String[] { "commons-compress", "commons-csv", "commons-dbcp", "commons-fileupload", "commons-jcs",
      // "commons-imaging", "commons-io", "commons-numbers", "commons-pool", "commons-text", "httpcomponents-core", "k-9" }) {
      for (final String project : new String[] { "commons-csv", "commons-dbcp", "commons-fileupload", "commons-jcs",
            "commons-imaging", "commons-io", "commons-numbers", "commons-pool", "commons-text" }) {
         System.out.print(project + " & ");
         final int versions = GitUtils.getVersions(new File(projectsFolder, project));
         System.out.print(versions + " & ");
         stats[0].addValue(versions);
         final File executionFile = new File(dependencyFolder, "execute_" + project + ".json");
         if (executionFile.exists()) {
            final ExecutionData changedTests = FolderSearcher.MAPPER.readValue(executionFile, ExecutionData.class);
            System.out.print(changedTests.getVersions().size() + " & ");
            stats[1].addValue(changedTests.getVersions().size());
            int tests = 0;
            for (final TestSet testSet : changedTests.getVersions().values()) {
               tests += testSet.getTests().size();
            }
            System.out.print(tests + " & ");
            stats[2].addValue(tests);

            int changes = 0;
            int sourceChanges = 0;
            int sourceTests = 0;
            final File potentialChangeFolder = new File(changeFolder, project);
            if (potentialChangeFolder.exists()) {
               final File changefile = new File(potentialChangeFolder, project + ".json");
               final ProjectChanges measuredChanges = FolderSearcher.MAPPER.readValue(changefile, ProjectChanges.class);
               changes = measuredChanges.getChangeCount();

               final File changefileOnlysource = new File(propertyFolder, project + "/" + project + ".json");
               if (changefileOnlysource.exists()) {
                  final VersionChangeProperties measuredChangesOnlysource = FolderSearcher.MAPPER.readValue(changefileOnlysource, VersionChangeProperties.class);
                  for (final ChangeProperties changesAll : measuredChangesOnlysource.getVersions().values()) {
                     for (final List<ChangeProperty> method : changesAll.getProperties().values()) {
                        for (final ChangeProperty nowMetho : method) {
                           if (nowMetho.isAffectsSource()) {
                              sourceChanges++;
                           }
                        }
                     }
                  }
                  // sourceChanges = measuredChangesOnlysource.getChangeCount();
               }
            }

            final File allTestProps = new File(propertyFolder, project + "/" + project + "_all.json");
            if (allTestProps.exists()) {
               final VersionChangeProperties properties = FolderSearcher.MAPPER.readValue(allTestProps, VersionChangeProperties.class);
               sourceTests = properties.getSourceChanges();
            }

            // System.out.print(sourceTests + " & ");
            // stats[3].addValue(sourceTests);

            System.out.print(changes + " & ");
            stats[4].addValue(changes);

            System.out.print(sourceChanges + " & ");
            stats[5].addValue(sourceChanges);

            final File changeTestProperties = new File(propertyFolder, project + File.separator + project + ".json");
            if (changeTestProperties.exists()) {
               final VersionChangeProperties versionProperties = FolderSearcher.MAPPER.readValue(changeTestProperties, VersionChangeProperties.class);

               final ProjectStatistics projectStatistics = new ProjectStatistics();
               versionProperties.executeProcessor(projectStatistics);

               System.out.print(new DecimalFormat("##.##").format(projectStatistics.affectedLines.getMean()) + " & ");
               stats[6].addValue(projectStatistics.affectedLines.getMean());
               System.out.print(new DecimalFormat("##.##").format(projectStatistics.calls.getMean()) + " & ");
               stats[7].addValue(projectStatistics.calls.getMean());
               final double durationMeanChange = projectStatistics.changes.getMean();
               System.out.print(new DecimalFormat("##.##").format(durationMeanChange) + " \\% ");
               stats[8].addValue(durationMeanChange);
            }

         }
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
