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

public class ChangeReader {
   
   private static final Logger LOG = LogManager.getLogger(ChangeReader.class);

   private int folderMeasurements = 0;

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
      int measurements = 0;
      int testcases = 0;
      LOG.debug("Reading from " + measurementFolder.getAbsolutePath());
      for (final File file : measurementFolder.listFiles()) {
         if (file.getName().endsWith(".xml")) {
            final Kopemedata data = new XMLDataLoader(file).getFullData();
            for (final TestcaseType testcaseMethod : data.getTestcases().getTestcase()) {
               System.out.println(file.getAbsolutePath());
               readTestcase(measurementFolder.getName(), data, testcaseMethod, changes, info);
               measurements += testcaseMethod.getDatacollector().get(0).getChunk().size();
               testcases++;
            }
         }
      }
      changes.setTestcaseCount(measurements);
      changes.setVersionCount(info.getStatistics().size());
      final String executorName = measurementFolder.getName().substring(measurementFolder.getName().lastIndexOf(File.separator) + 1);
      final File resultfile = new File(AnalysisUtil.getProjectResultFolder(), executorName + ".json");
      final File statisticFile = new File(statisticsFolder, executorName + ".json");
      try {
         FolderSearcher.MAPPER.writeValue(resultfile, changes);
         FolderSearcher.MAPPER.writeValue(statisticFile, info);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      System.out.println("Measurements: " + folderMeasurements + " Tests: " + testcases + " (last read: " + measurementFolder + ")");
      return changes;
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
            if (desc1.getN() > 3) {
               final TestcaseStatistic statistic = new TestcaseStatistic(desc1.getMean(), desc2.getMean(),
                     desc1.getStandardDeviation() / desc1.getMean(), desc2.getStandardDeviation() / desc2.getMean(), desc1.getN(),
                     desc1.getN() > 2 ? TestUtils.t(desc1, desc2) : 0);
               statistic.setPredecessor(versions[0]);
               // if (! (statistic.getTvalue() == Double.NaN)){
               final boolean isTChange = new TTest().tTest(desc1, desc2, 0.01);
               final Relation confidenceResult = ConfidenceIntervalInterpretion.compare(previous, current);
               final TestCase testcase = new TestCase(data.getTestcases().getClazz(), testcaseMethod.getName());
               allData.addStatistic(versions[1], testcase, fileName, statistic,
                     isTChange,
                     !confidenceResult.equals(Relation.EQUAL));
               if (isTChange || !confidenceResult.equals(Relation.EQUAL)) {
                  final double diff = (((desc1.getMean() - desc2.getMean()) * 10000) / desc1.getMean()) / 100;
                  changeKnowledge.addChange(testcase, versions[1],
                        confidenceResult,
                        isTChange ? Relation.GREATER_THAN : Relation.EQUAL,
                        desc1.getMean(), diff, statistic.getTvalue());
               }
               info.addMeasurement(versions[1], testcase, statistic);
            }else {
               System.out.println("Too few measurements: " + desc1.getN() + " " + versions[0] + " " + versions[1] );
            }
         }
      }
      return folderMeasurements;
   }

   public VersionData getAllData() {
      return allData;
   }
}
