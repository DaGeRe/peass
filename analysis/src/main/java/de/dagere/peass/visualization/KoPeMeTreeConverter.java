package de.dagere.peass.visualization;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata.Value;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.folders.CauseSearchFolders;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.measurement.rca.serialization.MeasuredValues;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;

/**
 * Read the overall data of the specified testcase (i.e. the values from the KoPeMe outside-measurement, no the Kieker in-method-measurement)
 */
public class KoPeMeTreeConverter {

   private static final Logger LOG = LogManager.getLogger(KoPeMeTreeConverter.class);

   private final GraphNode node;
   private int calls = 0, callsOld = 0;
   private final DescriptiveStatistics statisticsCurrent = new DescriptiveStatistics();
   private final DescriptiveStatistics statisticsOld = new DescriptiveStatistics();

   public KoPeMeTreeConverter(final CauseSearchFolders folders, final String version, final TestCase testcase) throws JAXBException {
      node = new GraphNode("overall", "public overall.overall()", "public overall.overall()");
      node.setVmValues(new MeasuredValues());
      node.setVmValuesPredecessor(new MeasuredValues());

      readStatistics(folders, version, testcase);
   }

   public KoPeMeTreeConverter(final PeassFolders folders, final String version, final TestCase testcase) throws JAXBException {
      this(folders.getDetailResultFolder(), version, testcase);
   }

   public KoPeMeTreeConverter(final File detailResultFolder, final String version, final TestCase testcase) throws JAXBException {
      File versionFolder = new File(detailResultFolder, testcase.getClazz() + File.separator + version);
      if (versionFolder.exists()) {
         node = new GraphNode("overall", "public overall.overall()", "public overall.overall()");
         node.setVmValues(new MeasuredValues());
         node.setVmValuesPredecessor(new MeasuredValues());
         for (File measuredVersionFolder : versionFolder.listFiles()) {
            for (File xmlFolder : measuredVersionFolder.listFiles((FileFilter) new WildcardFileFilter(testcase.getMethod() + "*xml"))) {

               readFile(version, testcase, measuredVersionFolder.getName(), xmlFolder);
            }
         }
         final TestcaseStatistic overallStatistic = new TestcaseStatistic(statisticsOld, statisticsCurrent, callsOld, calls);
         node.setStatistic(overallStatistic);
      } else {
         node = null;
      }
   }

   private void readStatistics(final CauseSearchFolders folders, final String version, final TestCase testcase) throws JAXBException {
      for (File versionFolder : folders.getArchiveResultFolder(version, testcase).listFiles()) {
         File levelFolder = new File(versionFolder, "0"); // For the beginning, just analyze topmost KoPeMe-measurement
         for (File kopemeFile : levelFolder.listFiles((FilenameFilter) new WildcardFileFilter(testcase.getMethod() + "*.xml"))) {
            readFile(version, testcase, versionFolder.getName(), kopemeFile);
         }
      }

      final TestcaseStatistic overallStatistic = new TestcaseStatistic(statisticsOld, statisticsCurrent, callsOld, calls);
      node.setStatistic(overallStatistic);

      node.setValues(statisticsCurrent.getValues());
      node.setValuesPredecessor(statisticsOld.getValues());
   }

   private void readFile(final String version, final TestCase testcase, final String currentVersion, final File kopemeFile)
         throws JAXBException {
      String stringIndex = kopemeFile.getName().substring(testcase.getMethodWithParams().length() + 1, kopemeFile.getName().lastIndexOf('_'));
      if (!stringIndex.matches("[0-9]+")) {
         LOG.error("Could not read file: {}", kopemeFile);
      } else {
         int index = Integer.parseInt(stringIndex);
         Kopemedata data = XMLDataLoader.loadData(kopemeFile);
         final Datacollector datacollector = data.getTestcases().getTestcase().get(0).getDatacollector().get(0);
         if (datacollector.getChunk().size() > 0) {
            for (Result result : datacollector.getChunk().get(0).getResult()) {
               readResult(version, currentVersion, result, index);
            }
         }
         for (Result result : datacollector.getResult()) {
            readResult(version, currentVersion, result, index);
         }
      }
   }

   private void readResult(final String version, final String currentVersion, final Result result, final int index) {
      if (currentVersion.equals(version)) {
         statisticsCurrent.addValue(result.getValue() / result.getRepetitions());
         calls += result.getIterations();

         List<StatisticalSummary> course = getCourse(result);
         node.getVmValues().getValues().put(index, course);
      } else {
         statisticsOld.addValue(result.getValue() / result.getRepetitions());
         callsOld += result.getIterations();

         List<StatisticalSummary> course = getCourse(result);
         node.getVmValuesPredecessor().getValues().put(index, course);
      }
   }

   private List<StatisticalSummary> getCourse(final Result result) {
      List<StatisticalSummary> course = new LinkedList<>();
      for (Value value : result.getFulldata().getValue()) {
         double mean = ((double) value.getValue()) / result.getRepetitions();
         final StatisticalSummaryValues statistic = new StatisticalSummaryValues(mean, 0, result.getRepetitions(), mean, mean, mean * result.getRepetitions());
         course.add(statistic);
      }
      return course;
   }

   public GraphNode getData() {
      return node;
   }

}
