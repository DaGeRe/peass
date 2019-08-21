package de.peass.analysis.changes;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.analysis.all.RepoFolders;
import de.peass.analysis.statistics.ConfidenceIntervalInterpretion;
import de.peass.confidence.KoPeMeDataHelper;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.analysis.Relation;
import de.peass.measurement.analysis.statistics.DescribedChunk;
import de.peass.measurement.analysis.statistics.TestcaseStatistic;
import de.peran.FolderSearcher;
import de.peran.analysis.helper.read.VersionData;
import de.peran.measurement.analysis.ProjectStatistics;

/**
 * Reads changes of fulldata - data need to be cleaned!
 * 
 * @author reichelt
 *
 */
public class ChangeReader {

   private static final Logger LOG = LogManager.getLogger(ChangeReader.class);

   private int folderMeasurements = 0;
   private int measurements = 0;
   private int testcases = 0;

   public double type1error = 0.01;
   private double type2error = 0.01;

   private double minChange = 2;

   private final VersionData allData = new VersionData();
   // private static VersionKnowledge oldKnowledge;
   private final File statisticsFolder;

   public ChangeReader(final RepoFolders resultsFolder, final String projectName) {
      statisticsFolder = resultsFolder.getProjectStatisticsFolder(projectName);
   }

   public ChangeReader(final File statisticsFolder, final String projectName) {
      this.statisticsFolder = statisticsFolder;
   }

   public double getType1error() {
      return type1error;
   }

   public void setType1error(final double type1error) {
      this.type1error = type1error;
   }

   public double getType2error() {
      return type2error;
   }

   public void setType2error(final double type2error) {
      this.type2error = type2error;
   }

   public ProjectChanges readFile(final File measurementFolder) throws JAXBException {
      final ProjectChanges changes = new ProjectChanges();
      final ProjectStatistics info = new ProjectStatistics();
      LOG.debug("Reading from " + measurementFolder.getAbsolutePath());
      readFile(measurementFolder, changes, info);

      changes.setTestcaseCount(measurements);
      changes.setVersionCount(info.getStatistics().size());
      writeResults(measurementFolder, changes, info);
      System.out.println("Measurements: " + folderMeasurements + " Tests: " + testcases + " (last read: " + measurementFolder + ")");
      return changes;
   }

   private void readFile(final File measurementFolder, final ProjectChanges changes, final ProjectStatistics info) throws JAXBException {
      if (measurementFolder.isDirectory()) {
         for (final File file : measurementFolder.listFiles()) {
            if (file.getName().endsWith(".xml")) {
               readFile(measurementFolder, changes, info, file);
            }
         }
      } else {
         if (measurementFolder.getName().endsWith(".xml")) {
            readFile(measurementFolder, changes, info, measurementFolder);
         }
      }
   }

   private void writeResults(final File measurementFolder, final ProjectChanges changes, final ProjectStatistics info) {
      final String measurementFolderName = measurementFolder.getName();
      final String executorName = measurementFolderName.substring(measurementFolderName.lastIndexOf(File.separator) + 1);
      final File resultfile = new File(statisticsFolder.getParentFile(), executorName + ".json");
      final File statisticFile = new File(statisticsFolder, executorName + ".json");
      try {
         FolderSearcher.MAPPER.writeValue(resultfile, changes);
         FolderSearcher.MAPPER.writeValue(statisticFile, info);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   private void readFile(final File measurementFolder, final ProjectChanges changes, final ProjectStatistics info, final File file) throws JAXBException {
      final Kopemedata data = new XMLDataLoader(file).getFullData();
      for (final TestcaseType testcaseMethod : data.getTestcases().getTestcase()) {
         System.out.println(file.getAbsolutePath());
         readTestcase(measurementFolder.getName(), data, testcaseMethod, changes, info);
         measurements += testcaseMethod.getDatacollector().get(0).getChunk().size();
         testcases++;
      }
   }

   private int readTestcase(final String fileName, final Kopemedata data, final TestcaseType testcaseMethod, final ProjectChanges changeKnowledge,
         final ProjectStatistics info) {
      for (final Chunk chunk : testcaseMethod.getDatacollector().get(0).getChunk()) {

         folderMeasurements++;
         final String[] versions = KoPeMeDataHelper.getVersions(chunk);
         LOG.debug(versions[1]);
         if (versions[1] != null) {
            final DescribedChunk describedChunk = new DescribedChunk(chunk, versions[0], versions[1]);
            describedChunk.removeOutliers();

            if (describedChunk.getDescPrevious().getN() > 3 && describedChunk.getDescCurrent().getN() > 3) {
               getIsChange(fileName, data, changeKnowledge, info, versions, describedChunk);
            } else {
               System.out.println("Too few measurements: " + describedChunk.getDescPrevious().getN() + " " + versions[0] + " " + versions[1]);
            }
         }
      }
      return folderMeasurements;
   }

   public void getIsChange(final String fileName, final Kopemedata data, final ProjectChanges changeKnowledge, final ProjectStatistics info,
         final String[] versions, final DescribedChunk describedChunk) {
      System.out.println(data.getTestcases().getClazz());
      final TestcaseStatistic statistic = describedChunk.getStatistic(type1error, type2error);
      statistic.setPredecessor(versions[0]);
      // if (! (statistic.getTvalue() == Double.NaN)){
      final Relation confidenceResult = ConfidenceIntervalInterpretion.compare(describedChunk.getPrevious(), describedChunk.getCurrent());
      final TestCase testcase = new TestCase(data);
      final double diff = describedChunk.getDiff();
      final boolean isBigEnoughDiff = Math.abs(diff) > minChange;
      allData.addStatistic(versions[1], testcase, fileName, statistic,
            statistic.isChange() && isBigEnoughDiff,
            !confidenceResult.equals(Relation.EQUAL));
      if (statistic.isChange() && isBigEnoughDiff) {
         changeKnowledge.addChange(testcase, versions[1],
               confidenceResult,
               statistic.isChange() ? Relation.GREATER_THAN : Relation.EQUAL,
               describedChunk.getDescPrevious().getMean(), diff,
               statistic.getTvalue(),
               statistic.getVMs());
      }
      info.addMeasurement(versions[1], testcase, statistic);
   }

   public VersionData getAllData() {
      return allData;
   }

   public double getMinChange() {
      return minChange;
   }

   public void setMinChange(final double minChange) {
      this.minChange = minChange;
   }
}
