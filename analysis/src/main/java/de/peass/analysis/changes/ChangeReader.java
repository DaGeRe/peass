package de.peass.analysis.changes;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import javax.xml.bind.JAXBException;

import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.analysis.all.RepoFolders;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.analysis.StatisticUtil;
import de.peass.measurement.analysis.statistics.DescribedChunk;
import de.peass.measurement.analysis.statistics.TestcaseStatistic;
import de.peran.FolderSearcher;
import de.peran.analysis.helper.read.VersionData;
import de.peran.measurement.analysis.ProjectStatistics;
import de.peran.measurement.analysis.statistics.ConfidenceIntervalInterpretion;
import de.peran.measurement.analysis.statistics.Relation;

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
   private double confidence = 0.01;

   private final VersionData allData = new VersionData();
   // private static VersionKnowledge oldKnowledge;
   private final File statisticsFolder;

   public ChangeReader(final RepoFolders resultsFolder, final String projectName) {
      statisticsFolder = resultsFolder.getProjectStatisticsFolder(projectName);
   }

   public ChangeReader(final File statisticsFolder, final String projectName) {
      this.statisticsFolder = statisticsFolder;
   }

   public double getConfidence() {
      return confidence;
   }

   public void setConfidence(final double confidence) {
      this.confidence = confidence;
   }

   public ProjectChanges readFile(final File measurementFolder) throws JAXBException {
      final ProjectChanges changes = new ProjectChanges();
      final ProjectStatistics info = new ProjectStatistics();
      LOG.debug("Reading from " + measurementFolder.getAbsolutePath());
      for (final File file : measurementFolder.listFiles()) {
         if (file.getName().endsWith(".xml")) {
            readFile(measurementFolder, changes, info, file);
         }
      }
      changes.setTestcaseCount(measurements);
      changes.setVersionCount(info.getStatistics().size());
      writeResults(measurementFolder, changes, info);
      System.out.println("Measurements: " + folderMeasurements + " Tests: " + testcases + " (last read: " + measurementFolder + ")");
      return changes;
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
         final String[] versions = new String[2];
         final Iterator<Result> iterator = chunk.getResult().iterator();
         versions[0] = iterator.next().getVersion().getGitversion();
         if (iterator.hasNext()) {
            while (iterator.hasNext()) {
               final Result r = iterator.next();
               if (!r.getVersion().getGitversion().equals(versions[0])) {
                  versions[1] = r.getVersion().getGitversion();
                  break;
               }
            }
            final DescribedChunk describedChunk = new DescribedChunk(chunk, versions[0], versions[1]);

            // final DescriptiveStatistics desc1 = new DescriptiveStatistics();
            // final DescriptiveStatistics desc2 = new DescriptiveStatistics();
            //
            // final List<Result> previous = new LinkedList<>();
            // final List<Result> current = new LinkedList<>();
            // for (final Result result : chunk.getResult()) {
            // if (result.getVersion().getGitversion().equals(versions[0]) && !Double.isNaN(result.getValue())) {
            // desc1.addValue(result.getValue());
            // previous.add(result);
            // }
            // if (result.getVersion().getGitversion().equals(versions[1]) && !Double.isNaN(result.getValue())) {
            // desc2.addValue(result.getValue());
            // current.add(result);
            // }
            // }
            if (describedChunk.getDesc1().getN() > 3 && describedChunk.getDesc2().getN() > 3) {
               if (testcaseMethod.getName().contains("testFolded") && versions[0].startsWith("4ed")) {
                  System.out.println("test");
               }
               getIsChange(fileName, data, testcaseMethod, changeKnowledge, info, versions, describedChunk);
            } else {
               System.out.println("Too few measurements: " + describedChunk.getDesc1().getN() + " " + versions[0] + " " + versions[1]);
            }
         }
      }
      return folderMeasurements;
   }

   public void getIsChange(final String fileName, final Kopemedata data, final TestcaseType testcaseMethod, final ProjectChanges changeKnowledge, final ProjectStatistics info,
         final String[] versions, final DescribedChunk describedChunk) {
      final boolean isChange = StatisticUtil.agnosticTTest(describedChunk.getDesc1(), describedChunk.getDesc2(), confidence, confidence) == de.peass.measurement.analysis.StatisticUtil.Relation.UNEQUAL;
//      final boolean isChange = TestUtils.tTest(describedChunk.getDesc1(), describedChunk.getDesc2(), confidence);
      System.out.println(data.getTestcases().getClazz());
      final TestcaseStatistic statistic = describedChunk.getStatistic(confidence);
      statistic.setPredecessor(versions[0]);
      // if (! (statistic.getTvalue() == Double.NaN)){
      final Relation confidenceResult = ConfidenceIntervalInterpretion.compare(describedChunk.getPrevious(), describedChunk.getCurrent());
      final TestCase testcase = new TestCase(data.getTestcases().getClazz(), testcaseMethod.getName());
      allData.addStatistic(versions[1], testcase, fileName, statistic,
            isChange,
            !confidenceResult.equals(Relation.EQUAL));
      if (isChange) {
         final double diff = (((describedChunk.getDesc1().getMean() - describedChunk.getDesc2().getMean()) * 10000) / describedChunk.getDesc1().getMean()) / 100;
         changeKnowledge.addChange(testcase, versions[1],
               confidenceResult,
               isChange ? Relation.GREATER_THAN : Relation.EQUAL,
               describedChunk.getDesc1().getMean(), diff, 
               statistic.getTvalue(),
               statistic.getVMs());
      }
      info.addMeasurement(versions[1], testcase, statistic);
   }

   public VersionData getAllData() {
      return allData;
   }
}
