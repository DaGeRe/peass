package de.peass.measurement.analysis.statistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata.Value;
import de.dagere.kopeme.generated.TestcaseType;

/**
 * Saves all data, its means and its coefficient of variation (CoV) for different executions of one test in one version.
 * 
 * @author reichelt
 *
 */
public class MeanCoVDataContinous extends MeanCoVData {

   public List<DescriptiveStatistics> getAllMeans() {
      return allMeans;
   }

   public List<DescriptiveStatistics> getAllCoVs() {
      return allCoVs;
   }

   public MeanCoVDataContinous(final TestcaseType testcase, int avg_count) {
      super(testcase, avg_count);
   }

   @Override
   protected void addTestcaseData() {
      for (final Result result : results) {
         for (int startindex = 0; startindex < result.getFulldata().getValue().size(); startindex++) {
            DescriptiveStatistics lastMean = new DescriptiveStatistics();
            for (int covIndex = 0; covIndex < Math.min(avgCount, startindex); covIndex++) {
               Value currentValueObject = result.getFulldata().getValue().get(startindex - covIndex);
               double currentValue = Double.parseDouble(currentValueObject.getValue());
               lastMean.addValue(currentValue);
            }
            addValue(startindex, lastMean.getStandardDeviation(), allCoVs);
            addValue(startindex, lastMean.getMean(), allMeans);
         }
      }
   }
   
   public File printAverages(final File folder, final String clazzname) throws IOException {
      final File summaryFile = new File(folder, "result_" + clazzname + "_" + testMethodName + "_all.csv");
      printAverages(summaryFile);

      System.out.println("set title 'Mean Mean and Mean Coefficient of Variation for " + clazzname + "." + testMethodName + "'");
      System.out.println(
            "plot '" + summaryFile.getName() + "' u 0:1 title 'Mean', '" + summaryFile.getName() + "' u 0:2 title 'CoV' axes x1y2");
      System.out.println();
      return summaryFile;
   }

}