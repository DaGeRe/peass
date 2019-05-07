package de.peass.analysis.changes;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.TestCleaner;
import de.peass.dependency.analysis.data.TestCase;
import de.peran.FolderSearcher;
import de.peran.analysis.helper.AnalysisUtil;
import de.peran.analysis.helper.read.VersionData;
import de.peran.measurement.analysis.TestcaseStatistic;
import de.peran.measurement.analysis.ProjectStatistics;
import de.peran.measurement.analysis.statistics.ConfidenceIntervalInterpretion;
import de.peran.measurement.analysis.statistics.Relation;

/**
 * Reads changes of fulldata - data need to be cleaned!
 * @author reichelt
 *
 */
public class ChangeReader {
   
   private static final Logger LOG = LogManager.getLogger(ChangeReader.class);

   private int folderMeasurements = 0;
   int measurements = 0;
   int testcases = 0;

   private final VersionData allData = new VersionData();
   // private static VersionKnowledge oldKnowledge;
   private final File statisticsFolder;

   public ChangeReader(final File resultsFolder, final String projectName) {
      AnalysisUtil.setProjectName(resultsFolder, projectName);
      statisticsFolder = new File(AnalysisUtil.getProjectResultFolder(), "statistics");
      if (!statisticsFolder.exists()) {
         statisticsFolder.mkdirs();
      }
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
      final String executorName = measurementFolder.getName().substring(measurementFolder.getName().lastIndexOf(File.separator) + 1);
      final File resultfile = new File(AnalysisUtil.getProjectResultFolder(), executorName + ".json");
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
            final DescriptiveStatistics desc1 = new DescriptiveStatistics();
            final DescriptiveStatistics desc2 = new DescriptiveStatistics();

            final List<Result> previous = new LinkedList<>();
            final List<Result> current = new LinkedList<>();
            for (final Result result : chunk.getResult()) {
               if (result.getVersion().getGitversion().equals(versions[0]) && !Double.isNaN(result.getValue())) {
                  desc1.addValue(result.getValue());
                  previous.add(result);
               }
               if (result.getVersion().getGitversion().equals(versions[1]) && !Double.isNaN(result.getValue())) {
                  desc2.addValue(result.getValue());
                  current.add(result);
               }
            }
            if (desc1.getN() > 3 && desc2.getN() > 3) {
               getIsChange(fileName, data, testcaseMethod, changeKnowledge, info, versions, desc1, desc2, previous, current);
            }else {
               System.out.println("Too few measurements: " + desc1.getN() + " " + versions[0] + " " + versions[1] );
            }
         }
      }
      return folderMeasurements;
   }

   public void getIsChange(final String fileName, final Kopemedata data, final TestcaseType testcaseMethod, final ProjectChanges changeKnowledge, final ProjectStatistics info,
         final String[] versions, final DescriptiveStatistics desc1, final DescriptiveStatistics desc2, final List<Result> previous, final List<Result> current) {
      boolean isChange = TestUtils.tTest(desc1, desc2, 0.005);
      System.out.println(data.getTestcases().getClazz());
      final TestcaseStatistic statistic = new TestcaseStatistic(desc1.getMean(), desc2.getMean(),
            desc1.getStandardDeviation() / desc1.getMean(), desc2.getStandardDeviation() / desc2.getMean(), desc1.getN(),
            desc1.getN() > 2 ? TestUtils.t(desc1, desc2) : 0, isChange);
      statistic.setPredecessor(versions[0]);
      // if (! (statistic.getTvalue() == Double.NaN)){
      final boolean isTChange = new TTest().tTest(desc1, desc2, 0.01);
      final Relation confidenceResult = ConfidenceIntervalInterpretion.compare(previous, current);
      final TestCase testcase = new TestCase(data.getTestcases().getClazz(), testcaseMethod.getName());
      allData.addStatistic(versions[1], testcase, fileName, statistic,
            isTChange,
            !confidenceResult.equals(Relation.EQUAL));
      if (isTChange) {
         final double diff = (((desc1.getMean() - desc2.getMean()) * 10000) / desc1.getMean()) / 100;
         changeKnowledge.addChange(testcase, versions[1],
               confidenceResult,
               isTChange ? Relation.GREATER_THAN : Relation.EQUAL,
               desc1.getMean(), diff, statistic.getTvalue(), 
               statistic.getExecutions());
      }
      info.addMeasurement(versions[1], testcase, statistic);
   }

   public VersionData getAllData() {
      return allData;
   }
}
