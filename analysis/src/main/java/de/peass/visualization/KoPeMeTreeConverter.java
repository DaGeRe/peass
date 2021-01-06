package de.peass.visualization;

import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata.Value;
import de.dagere.kopeme.generated.Versioninfo;
import de.dagere.kopeme.generated.TestcaseType.Datacollector;
import de.peass.dependency.CauseSearchFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.analysis.statistics.TestcaseStatistic;
import de.peass.measurement.rca.serialization.MeasuredValues;

public class KoPeMeTreeConverter {

   private static final int NANO_TO_MICRO = 1000;
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

   private void readStatistics(final CauseSearchFolders folders, final String version, final TestCase testcase) throws JAXBException {
      for (File versionFolder : folders.getArchiveResultFolder(version, testcase).listFiles()) {
         File levelFolder = new File(versionFolder, "0"); // For the beginning, just analyze topmost KoPeMe-measurement
         for (File kopemeFile : levelFolder.listFiles((FilenameFilter) new WildcardFileFilter(testcase.getMethod() + "*.xml"))) {
            readFile(version, testcase, versionFolder, kopemeFile);
         }
      }

      final TestcaseStatistic overallStatistic = new TestcaseStatistic(statisticsOld, statisticsCurrent, callsOld, calls);
      node.setStatistic(overallStatistic);

      node.setValues(statisticsCurrent.getValues());
      node.setValuesPredecessor(statisticsOld.getValues());
   }

   private void readFile(final String version, final TestCase testcase, File versionFolder, File kopemeFile)
         throws JAXBException {
      String stringIndex = kopemeFile.getName().substring(testcase.getMethod().length() + 1, kopemeFile.getName().lastIndexOf('_'));
      int index = Integer.parseInt(stringIndex);
      Kopemedata data = XMLDataLoader.loadData(kopemeFile);
      final Datacollector datacollector = data.getTestcases().getTestcase().get(0).getDatacollector().get(0);
      if (datacollector.getChunk().size() > 0) {
         for (Result result : datacollector.getChunk().get(0).getResult()) {
            readResult(version, versionFolder.getName(), result, index);
         }
      }
      for (Result result : datacollector.getResult()) {
         readResult(version, versionFolder.getName(), result, index);
      }
   }

   private void readResult(final String version, String currentVersion, Result result, int index) {
      if (currentVersion.equals(version)) {
         statisticsCurrent.addValue(result.getValue() / result.getRepetitions() / NANO_TO_MICRO);
         calls += result.getIterations();

         List<StatisticalSummary> course = getCourse(result);
         node.getVmValues().getValues().put(index, course);
      } else {
         statisticsOld.addValue(result.getValue() / result.getRepetitions() / NANO_TO_MICRO);
         callsOld += result.getIterations();

         List<StatisticalSummary> course = getCourse(result);
         node.getVmValuesPredecessor().getValues().put(index, course);
      }
   }

   private List<StatisticalSummary> getCourse(Result result) {
      List<StatisticalSummary> course = new LinkedList<>();
      for (Value value : result.getFulldata().getValue()) {
         double mean = ((double) value.getValue()) / result.getRepetitions() / NANO_TO_MICRO; // Convert nanoseconds (from KoPeMe) to microseconds
         final StatisticalSummaryValues statistic = new StatisticalSummaryValues(mean, 0, result.getRepetitions(), mean, mean, mean * result.getRepetitions());
         course.add(statistic);
      }
      return course;
   }

   public GraphNode getData() {
      return node;
   }

}
