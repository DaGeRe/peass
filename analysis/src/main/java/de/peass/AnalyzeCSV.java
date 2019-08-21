package de.peass;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;

public class AnalyzeCSV {

   static class Version {
      final List<Run> runs = new LinkedList<>();

      DescriptiveStatistics getStatistics(int warmup) {
         final DescriptiveStatistics statistics = new DescriptiveStatistics();
         for (Run run : runs) {
            final List<Long> values = run.getSubArray(warmup);
            final double result = values.stream().mapToDouble(l -> l).average().getAsDouble();
            statistics.addValue(result);
         }
         return statistics;
      }

      DescriptiveStatistics getStatisticsBoostrap(int warmup) {
         final int randomCount = 10;
         final int bootstrapCount = 100;
         final DescriptiveStatistics statistics = getStatistics(warmup);
         final double[] averages = getAverages(warmup);
         for (int i = 0; i < bootstrapCount; i++) {
            double sum = 0;
            for (int j = 0; j < randomCount; j++) {
               int randomVal = new Random().nextInt(averages.length);
               sum += averages[randomVal];
            }
            double avg = sum / randomCount;
            
            statistics.addValue(avg);
         }
         return statistics;
      }

      private double[] getAverages(int warmup) {
         double[] averages = new double[runs.size()];
         for (int i = 0; i < averages.length; i++) {
            final List<Long> values = runs.get(i).getSubArray(warmup);
            final double result = values.stream().mapToDouble(l -> l).average().getAsDouble();
            averages[i] = result;
         }
         return averages;
      }
   }

   static class Run {
      private final List<Long> values;

      public Run(List<Long> values2) {
         this.values = values2;
      }

      List<Long> getSubArray(int warmup) {
         return values.subList(warmup, values.size());
      }
   }

   static int minExecutions = Integer.MAX_VALUE;

   public static void main(String[] args) throws NumberFormatException, IOException {
      File folder = new File(args[0]);
      File versionFolder = folder.listFiles()[0];

      File firstVersion = versionFolder.listFiles()[0];
      File secondVersion = versionFolder.listFiles()[1];

      Version version = readVersion(firstVersion);
      Version old = readVersion(secondVersion);

      System.out.println("Executions: " + minExecutions);

      for (int i = 1000; i < minExecutions; i += 1000) {
         DescriptiveStatistics stat1 = version.getStatistics(i);
         DescriptiveStatistics stat2 = old.getStatistics(i);
         double tVal = TestUtils.t(stat1, stat2);
         System.out.println(i + " " + tVal);
         
         DescriptiveStatistics stat1B = version.getStatisticsBoostrap(i);
         DescriptiveStatistics stat2B = old.getStatisticsBoostrap(i);
         double tValB = TestUtils.t(stat1B, stat2B);
         System.out.println("Boostrap" + i + " " + tValB);
      }
   }

   private static Version readVersion(File firstVersion) throws FileNotFoundException, IOException {
      Version version = new Version();
      for (File vmStart : firstVersion.listFiles((FileFilter) new RegexFileFilter("[0-9]+"))) {
         Run run = readRun(vmStart);
         version.runs.add(run);
      }
      return version;
   }

   private static Run readRun(File vmStart) throws FileNotFoundException, IOException {
      File methodFolder = vmStart.listFiles()[0];
      File kiekerFolder = methodFolder.listFiles()[0];
      File kiekerFile = kiekerFolder.listFiles((FileFilter) new RegexFileFilter("kieker-[0-9]+-[0-9]+-UTC-001.dat"))[0];
      try (BufferedReader reader = new BufferedReader(new FileReader(kiekerFile))) {
         String line;
         List<Long> values = new LinkedList<>();
         while ((line = reader.readLine()) != null) {
            String[] splittet = line.split(";");
            if (splittet[6].matches("[0-9]+") && splittet[5].matches("[0-9]+")) {
               long tout = Long.parseLong(splittet[6]);
               long tin = Long.parseLong(splittet[5]);
               long duration = tout - tin;
               // System.out.println(duration);
               values.add(duration);
            }
         }
         if (values.size() < minExecutions) {
            minExecutions = values.size();
         }
         Run run = new Run(values);
         return run;
      }
   }
}
