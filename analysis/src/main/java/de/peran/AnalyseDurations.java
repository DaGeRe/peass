package de.peran;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TTest;

import de.dagere.kopeme.datastorage.XMLDataLoader;
import de.dagere.kopeme.generated.Kopemedata;
import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.measurement.analysis.statistics.MeanCoVData;
import de.peran.breaksearch.helper.MinimalExecutionDeterminer;

class DebugInfo implements Comparable<DebugInfo> {
   String eventType, version, testcase;
   long start;

   public DebugInfo(final String type, final long start) {
      this.eventType = type;
      this.start = start;
   }

   public DebugInfo(final String type, final long start, final String version, final String testcase) {
      this.eventType = type;
      this.start = start;
      this.version = version;
      this.testcase = testcase;
   }

   @Override
   public int compareTo(final DebugInfo arg0) {
      return (int) (start - arg0.start);
   }
}

public class AnalyseDurations {

   public static void main(final String[] args) throws IOException, JAXBException {
      final File folder = new File("../measurement/scripts/versions/sync");
      for (final File computerFolder : folder.listFiles()) {
         // if (!computerFolder.getName().equals("r146")) {
         // System.out.println(computerFolder.getName());
         // continue;
         // }
         if (computerFolder.isDirectory()) {
            calculateDurations(computerFolder);

            final List<DebugInfo> debugInfos = new LinkedList<>();
            final File logDir = new File(computerFolder, "logs");
            if (logDir.exists()) {
               for (final File logfile : FileUtils.listFiles(logDir, new RegexFileFilter(".*txt"), TrueFileFilter.INSTANCE)) {
                  debugInfos.add(new DebugInfo("logend", logfile.lastModified()));
               }
            }
            System.out.println("Folder: " + computerFolder.getPath() + " Logs: " + debugInfos.size());
            for (final File datafile : FileUtils.listFiles(new File(computerFolder, "measurementsFull"), new RegexFileFilter(".*xml"), FalseFileFilter.INSTANCE)) {
               final Kopemedata data = new XMLDataLoader(datafile).getFullData();
               final String testcase = data.getTestcases().getClazz() + "#" + data.getTestcases().getTestcase().get(0).getName();
               for (final Result r : data.getTestcases().getTestcase().get(0).getDatacollector().get(0).getResult()) {
                  debugInfos.add(new DebugInfo("measurementEnd", r.getDate(), r.getVersion().getGitversion(), testcase));
               }
            }

            Collections.sort(debugInfos);
            try (final FileWriter fw = new FileWriter(new File(computerFolder, "events.csv"))) {
               for (final DebugInfo info : debugInfos) {
                  fw.write(info.start / 1000 + ";" + info.eventType);
                  if (info.version != null) {
                     fw.write(";" + info.version + ";" + info.testcase);
                  }
                  fw.write("\n");
               }
               fw.flush();
            }
         }
      }

   }

   private static void calculateDurations(final File computerFolder) throws IOException, JAXBException {
      final File measurementsFullFolder = new File(computerFolder, "measurementsFull");
      final Map<String, Map<String, TestcaseInfo>> durationMap = new TreeMap<>();
      System.out.println(measurementsFullFolder.getAbsolutePath());
      for (final File measurementFile : measurementsFullFolder.listFiles()) {
         if (measurementFile.getName().endsWith(".xml")) {
            final Kopemedata data = new XMLDataLoader(measurementFile).getFullData();
            final String testcase = data.getTestcases().getClazz() + "#" + data.getTestcases().getTestcase().get(0).getName();
            final Map<String, TestcaseInfo> testcaseMap = calculateTestcaseDurations(data);
            durationMap.put(testcase, testcaseMap);

         }
      }
      try (final FileWriter fw = new FileWriter(new File(computerFolder, computerFolder.getName() + ".csv"))) {
         // final File last = null;
         fw.write("#testcase;version;durationInMilliseconds;valueCount\n");
         for (final Map.Entry<String, Map<String, TestcaseInfo>> entry : durationMap.entrySet()) {
            for (final Map.Entry<String, TestcaseInfo> versionDuration : entry.getValue().entrySet()) {
               if (entry.getKey().endsWith("testFileCleanerDirectory_NullStrategy")) {
                  System.out.println(versionDuration.getKey() + ";" + versionDuration.getValue());
               }
               fw.write(entry.getKey() + ";" + versionDuration.getKey() + ";" + versionDuration.getValue().duration + ";" + versionDuration.getValue().count + ";"
                     + MeanCoVData.FORMAT.format(versionDuration.getValue().avg) + ";" +
                     MeanCoVData.FORMAT.format(100 * versionDuration.getValue().standarddeviation) + ";" +
                     MeanCoVData.FORMAT.format(versionDuration.getValue().tval) + "\n");
               fw.flush();
            }
         }

      }
   }

   static class TestcaseInfo {
      long duration;
      int count;
      double avg;
      double standarddeviation;
      double tval;

      public TestcaseInfo(long duration, int count, double avg, double standarddeviation, double tval) {
         super();
         this.duration = duration;
         this.count = count;
         this.avg = avg;
         this.standarddeviation = standarddeviation;
         this.tval = tval;
      }

   }

   private static Map<String, TestcaseInfo> calculateTestcaseDurations(final Kopemedata data) {
      final Map<String, TestcaseInfo> testcaseMap = new LinkedHashMap<>();
//      final String testcase = data.getTestcases().getClazz() + "#" + data.getTestcases().getTestcase().get(0).getName();
      for (final Chunk chunk : data.getTestcases().getTestcase().get(0).getDatacollector().get(0).getChunk()) {
         if (chunk.getResult().size() >= 2) {
            final Result last = chunk.getResult().get(chunk.getResult().size() - 1);
            final Result second = chunk.getResult().get(1);
            List<Result> dataNew = new LinkedList<>();
            List<Result> dataOld = new LinkedList<>();

            for (Result r : chunk.getResult()) {
               if (r.getVersion().getGitversion().equals(second.getVersion().getGitversion())) {
                  dataNew.add(r);
               } else {
                  dataOld.add(r);
               }
            }
            DescriptiveStatistics statNew = MinimalExecutionDeterminer.getStatistic(dataNew);
            DescriptiveStatistics statOld = MinimalExecutionDeterminer.getStatistic(dataOld);
            double tval;
            if (dataNew.size() > 2) {
               tval = new TTest().homoscedasticT(statNew, statOld);
            } else {
               tval = 0;
            }

            DescriptiveStatistics stat = MinimalExecutionDeterminer.getStatistic(chunk.getResult());
            // MeanCoVData dataSecond = new MeanCoVData("2", secondData);
            // MeanCoVData cov = new MeanCoVData("1", chunk.getResult().stream().filter(result -> result.getVersion().getGitversion().equals(second.getVersion().getGitversion())))
            testcaseMap.put(second.getVersion().getGitversion(),
                  new TestcaseInfo(last.getDate() - chunk.getChunkStartTime(),
                        chunk.getResult().size(),
                        stat.getMean(),
                        stat.getStandardDeviation() / stat.getMean(),
                        tval));
         }
      }
      return testcaseMap;
   }
}
