package de.peran.analysis.helper;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.inference.TestUtils;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.analysis.changes.ProjectChanges;
import de.peass.dependency.analysis.data.TestCase;
import de.peran.FolderSearcher;
import de.peran.analysis.helper.read.VersionData;
import de.peran.measurement.analysis.Statistic;
import de.peran.measurement.analysis.StatisticInfo;
import de.peran.measurement.analysis.statistics.ConfidenceIntervalInterpretion;
import de.peran.measurement.analysis.statistics.Relation;

public class DataReader {

   private int folderMeasurements = 0;

   private final VersionData allData = new VersionData();
   // private static VersionKnowledge oldKnowledge;
   private final File statisticsFolder;

   public DataReader(final File resultsFolder, final String projectName) {
      AnalysisUtil.setProjectName(resultsFolder, projectName);
      statisticsFolder = new File(AnalysisUtil.getProjectResultFolder(), "statistics");
      if (!statisticsFolder.exists()) {
         statisticsFolder.mkdirs();
      }

   }

   public ProjectChanges readFile(final File folder) throws JAXBException {
      // final File folder = new File(fileName);
      final ProjectChanges changeKnowledge = new ProjectChanges();
      final StatisticInfo info = new StatisticInfo();
      int testcases = 0;
      // for (final File measurementFolder : getFullMeasurementFiles(folder)) {
      for (final File file : folder.listFiles()) {
         if (file.getName().endsWith(".xml")) {
            final Kopemedata data = new XMLDataLoader(file).getFullData();
            for (final TestcaseType testcaseMethod : data.getTestcases().getTestcase()) {
               System.out.println(file.getAbsolutePath());
               readTestcase(folder.getName(), data, testcaseMethod, changeKnowledge, info);
               testcases += testcaseMethod.getDatacollector().get(0).getChunk().size();
            }
         }
      }
      // }
      changeKnowledge.setTestcaseCount(testcases);
      changeKnowledge.setVersionCount(info.getStatistics().size());
      final String executorName = folder.getName().substring(folder.getName().lastIndexOf(File.separator) + 1);
      final File resultfile = new File(AnalysisUtil.getProjectResultFolder(), executorName + ".json");
      final File statisticFile = new File(statisticsFolder, executorName + ".json");
      try {
         FolderSearcher.MAPPER.writeValue(resultfile, changeKnowledge);
         FolderSearcher.MAPPER.writeValue(statisticFile, info);
      } catch (final IOException e) {
         e.printStackTrace();
      }
      System.out.println("Measurements: " + folderMeasurements + " (last read: " + folder + ")");
      return changeKnowledge;
   }

   private int readTestcase(final String fileName, final Kopemedata data, final TestcaseType testcaseMethod, final ProjectChanges changeKnowledge,
         final StatisticInfo info) {
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
               if (result.getVersion().getGitversion().equals(versions[0])) {
                  desc1.addValue(result.getValue());
                  previous.add(result);
               }
               if (result.getVersion().getGitversion().equals(versions[1])) {
                  desc2.addValue(result.getValue());
                  current.add(result);
               }
            }
            if (desc1.getN() > 3) {
               final Statistic statistic = new Statistic(desc1.getMean(), desc2.getMean(),
                     desc1.getStandardDeviation() / desc1.getMean(), desc2.getStandardDeviation() / desc2.getMean(), desc1.getN(),
                     desc1.getN() > 2 ? TestUtils.t(desc1, desc2) : 0);
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
            }
         }
      }
      return folderMeasurements;
   }

   private static List<File> getFullMeasurementFiles(final File folder) {
      final List<File> result = new LinkedList<>();
      System.out.println(folder);
      for (final File subfolder : folder.listFiles()) {
         if (subfolder.getName().equals("measurementsFull")) {
            result.add(subfolder);
         } else {
            if (subfolder.isDirectory() && !subfolder.getName().equals("ignore")) {
               result.addAll(getFullMeasurementFiles(subfolder));
            }
         }
      }
      return result;
   }

   public VersionData getAllData() {
      return allData;
   }
}
