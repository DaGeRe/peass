package de.dagere.peass.analysis.measurement.statistics;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import de.dagere.kopeme.kopemedata.MeasuredValue;
import de.dagere.kopeme.kopemedata.TestMethod;
import de.dagere.kopeme.kopemedata.VMResult;

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

   public MeanCoVDataContinous(final TestMethod testcase, int avg_count) {
      super(testcase, avg_count);
   }

   @Override
   protected void addTestcaseData() {
      for (final VMResult result : results) {
         for (int startindex = 0; startindex < result.getFulldata().getValues().size(); startindex++) {
            DescriptiveStatistics lastMean = new DescriptiveStatistics();
            for (int covIndex = 0; covIndex < Math.min(avgCount, startindex); covIndex++) {
               MeasuredValue currentValueObject = result.getFulldata().getValues().get(startindex - covIndex);
               double currentValue = currentValueObject.getValue();
               lastMean.addValue(currentValue);
            }
            addValue(startindex, lastMean.getStandardDeviation(), allCoVs);
            addValue(startindex, lastMean.getMean(), allMeans);
         }
      }
   }
   
   protected void addValue(final int index, final double value, final List<DescriptiveStatistics> statistics) {
      DescriptiveStatistics meanSummary;
      if (statistics.size() <= index) {
         meanSummary = new DescriptiveStatistics();
         statistics.add(meanSummary);
      } else {
         meanSummary = statistics.get(index);
      }
      meanSummary.addValue(value);
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