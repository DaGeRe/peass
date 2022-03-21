package de.dagere.peass.analysis.measurement;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.peass.analysis.changes.ProjectChanges;
import de.dagere.peass.config.StatisticsConfig;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.measurement.dataloading.DataAnalyser;
import de.dagere.peass.measurement.statistics.Relation;
import de.dagere.peass.measurement.statistics.data.EvaluationPair;
import de.dagere.peass.measurement.statistics.data.TestData;
import de.dagere.peass.utils.Constants;

/**
 * Analyzes full data and tells which version contain changes based upon given statistical tests (confidence interval, MannWhitney, ..)
 * 
 * @author reichelt
 *
 */
public class AnalyseFullData extends DataAnalyser {

   private static final Logger LOG = LogManager.getLogger(AnalyseFullData.class);

   public Set<String> versions = new HashSet<>();
   public int testcases = 0;

   private final File changeFile;
   private final ProjectChanges projectChanges;
   private final ModuleClassMapping mapping;

   private final ProjectStatistics info;

   public AnalyseFullData(final File changesFile, final ProjectStatistics info, final ModuleClassMapping mapping, final StatisticsConfig statisticConfig) {
      this.changeFile = changesFile;
      this.mapping = mapping;
      this.info = info;
      projectChanges = new ProjectChanges(statisticConfig);
      LOG.info("Writing changes to: {}", changeFile.getAbsolutePath());
      try {
         Constants.OBJECTMAPPER.writeValue(changeFile, projectChanges);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   public ProjectChanges getProjectChanges() {
      return projectChanges;
   }

   @Override
   public void processTestdata(final TestData measurementEntry) {
      for (final Entry<String, EvaluationPair> versionEntry : measurementEntry.getMeasurements().entrySet()) {
         final String version = versionEntry.getKey();
         LOG.debug("Analysing: {} ({}#{}) Complete: {}", version, measurementEntry.getTestClass(), measurementEntry.getTestMethod(), versionEntry.getValue().isComplete());

         final TestStatistic teststatistic = new TestStatistic(versionEntry.getValue(), info, projectChanges.getStatisticsConfig());

         if (Constants.DRAW_RESULTS) {
            new PngWriter(changeFile).drawPNGs(measurementEntry, versionEntry, version, teststatistic);
         }

         LOG.debug("Change: {} T: {}", teststatistic.isChange(), teststatistic.getTValue());

         if (teststatistic.isChange()) {
            addChangeData(measurementEntry, versionEntry, version, teststatistic);

            try {
               Constants.OBJECTMAPPER.writeValue(changeFile, projectChanges);
            } catch (final IOException e) {
               e.printStackTrace();
            }

         }
      }
      versions.addAll(measurementEntry.getMeasurements().keySet());
      LOG.debug("Version: {}", measurementEntry.getMeasurements().keySet());
      testcases += measurementEntry.getMeasurements().size();
   }

   private void addChangeData(final TestData measurementEntry, final Entry<String, EvaluationPair> entry, final String version, final TestStatistic teststatistic) {
      Relation tRelation = teststatistic.getConfidenceResult();
      if (teststatistic.getDiff() > 0) {
         tRelation = Relation.LESS_THAN;
      } else {
         tRelation = Relation.GREATER_THAN;
      }

//      long repetitions = entry.getValue().getCurrent().get(0).getRepetitions();

      final double diffPercent = ((double) teststatistic.getDiff()) / 100;
      final double mean = teststatistic.getPreviousStatistic().getMean();

      final TestCase currentTest = getCurrentTestcase(measurementEntry);

      projectChanges.addChange(currentTest, version,
            teststatistic.getConfidenceResult(), tRelation, mean,
            diffPercent, teststatistic.getTValue(),
            teststatistic.getCurrentStatistic().getN());
      projectChanges.setVersionCount(versions.size());
      projectChanges.setTestcaseCount(testcases);

      LOG.info("Version: {} vs {} Klasse: {}#{}", version, entry.getValue().getPreviousVersion(), measurementEntry.getTestClass(),
            measurementEntry.getTestMethod());
      LOG.debug("Confidence Interval Comparison: {}", teststatistic.getConfidenceResult());

      System.out.println("git diff " + version + ".." + VersionComparator.getPreviousVersion(version));
   }

   private TestCase getCurrentTestcase(final TestData measurementEntry) {
      final TestCase currentTest;
      if (mapping != null) {
         final String module = mapping.getModuleOfClass(measurementEntry.getTestClass());
         currentTest = new TestCase(measurementEntry.getTestClass(), measurementEntry.getTestMethod(), module);
      } else {
         currentTest = measurementEntry.getTestCase();
      }
      return currentTest;
   }

   public static void main(final String[] args) throws InterruptedException, JAXBException, JsonParseException, JsonMappingException, IOException {
      final File folder = new File(args[0]);
      if (!folder.getName().equals("measurements")) {
         throw new RuntimeException("Can only be executed with measurements-folder! For searching folders, use FolderSearcher");
      }
      LOG.info("Draw results: " + Constants.DRAW_RESULTS);
      
      VersionComparator.setDependencies(new StaticTestSelection());
//      final File dependencyFile = new File(args[1]);
//      final Dependencies dependencies = Constants.OBJECTMAPPER.readValue(dependencyFile, Dependencies.class);
//      VersionComparator.setDependencies(dependencies);
//      throw new RuntimeException("adapt if needed");
      ProjectStatistics statistics = new ProjectStatistics();
      final AnalyseFullData analyseFullData = new AnalyseFullData(new File("results/changes.json"), statistics, null, new StatisticsConfig());
      analyseFullData.analyseFolder(folder);
      
      Constants.OBJECTMAPPER.writeValue(new File("results/statistics.json"), statistics);

   }

   public int getChanges() {
      return projectChanges.getChangeCount();
   }
}
