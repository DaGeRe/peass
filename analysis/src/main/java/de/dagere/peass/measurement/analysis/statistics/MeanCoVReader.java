package de.dagere.peass.measurement.analysis.statistics;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.Result.Fulldata.Value;

public class MeanCoVReader {
   
   protected final List<DescriptiveStatistics> allMeans;
   protected final List<DescriptiveStatistics> allCoVs;
   
   SummaryStatistics statistics = new SummaryStatistics();
   int index = 0;

   public MeanCoVReader(List<DescriptiveStatistics> allMeans, List<DescriptiveStatistics> allCoVs) {
      this.allMeans = allMeans;
      this.allCoVs = allCoVs;
   }

   public void addTestcaseData(List<Result> results, int avgCount) {
      for (final Result result : results) {
         for (final Value value : result.getFulldata().getValue()) {
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
