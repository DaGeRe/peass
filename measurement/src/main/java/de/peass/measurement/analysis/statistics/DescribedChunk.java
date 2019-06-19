package de.peass.measurement.analysis.statistics;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;

import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.measurement.analysis.StatisticUtil;

public class DescribedChunk {
   private final DescriptiveStatistics desc1 = new DescriptiveStatistics();
   private final DescriptiveStatistics desc2 = new DescriptiveStatistics();

   private final List<Result> previous = new LinkedList<>();
   private final List<Result> current = new LinkedList<>();

   public DescribedChunk(Chunk chunk, String versionPrevious, String versionCurrent) {
      for (final Result result : chunk.getResult()) {
         if (result.getVersion().getGitversion().equals(versionPrevious) && !Double.isNaN(result.getValue())) {
            desc1.addValue(result.getValue());
            previous.add(result);
         }
         if (result.getVersion().getGitversion().equals(versionCurrent) && !Double.isNaN(result.getValue())) {
            desc2.addValue(result.getValue());
            current.add(result);
         }
      }
   }

   public DescriptiveStatistics getDesc1() {
      return desc1;
   }

   public DescriptiveStatistics getDesc2() {
      return desc2;
   }

   public List<Result> getPrevious() {
      return previous;
   }

   public List<Result> getCurrent() {
      return current;
   }
   
   public TestcaseStatistic getStatistic(double confidence) {
      boolean isChange = StatisticUtil.agnosticTTest(desc1, desc2, confidence, confidence) == de.peass.measurement.analysis.StatisticUtil.Relation.UNEQUAL;
      final TestcaseStatistic statistic = new TestcaseStatistic(desc1.getMean(), desc2.getMean(),
            desc1.getStandardDeviation() / desc1.getMean(), desc2.getStandardDeviation() / desc2.getMean(), desc1.getN(),
            desc1.getN() > 2 ? TestUtils.t(desc1, desc2) : 0, isChange);
      return statistic;
   }
}
