package de.dagere.peass.analysis.measurement.statistics;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import de.dagere.kopeme.kopemedata.MeasuredValue;
import de.dagere.kopeme.kopemedata.VMResult;

public class MeanCoVReader {
   
   protected final List<DescriptiveStatistics> allMeans;
   protected final List<DescriptiveStatistics> allCoVs;
   
   SummaryStatistics statistics = new SummaryStatistics();
   int index = 0;

   public MeanCoVReader(List<DescriptiveStatistics> allMeans, List<DescriptiveStatistics> allCoVs) {
      this.allMeans = allMeans;
      this.allCoVs = allCoVs;
   }

   public void addTestcaseData(List<VMResult> results, int avgCount) {
      for (final VMResult result : results) {
         for (final MeasuredValue value : result.getFulldata().getValues()) {
            // writer.write(value.getValue() + "\n");
            statistics.addValue(value.getValue());
            if (statistics.getN() == avgCount) {
               nextIndex();
            }
         }
      }
   }

   private void nextIndex() {
      final double cov = statistics.getStandardDeviation() / statistics.getMean();
      addValue(index, statistics.getMean(), allMeans);
      addValue(index, cov, allCoVs);
      index++;
      statistics = new SummaryStatistics();
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
}
