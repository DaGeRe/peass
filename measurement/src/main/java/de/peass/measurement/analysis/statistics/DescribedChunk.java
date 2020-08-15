package de.peass.measurement.analysis.statistics;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.generated.Result;
import de.dagere.kopeme.generated.TestcaseType.Datacollector.Chunk;
import de.peass.measurement.analysis.StatisticUtil;

public class DescribedChunk {
   private static final Logger LOG = LogManager.getLogger(DescribedChunk.class);
   
   private final DescriptiveStatistics descPrev = new DescriptiveStatistics();
   private final DescriptiveStatistics descCurrent = new DescriptiveStatistics();

   private final List<Result> previous = new LinkedList<>();
   private final List<Result> current = new LinkedList<>();

   public DescribedChunk(final Chunk chunk, final String versionPrevious, final String versionCurrent) {
      long minRepetitions = Long.MAX_VALUE, minIterations = Long.MAX_VALUE;
      for (final Result result : chunk.getResult()) {
         if (!Double.isNaN(result.getValue())) {
            minRepetitions = Math.min(minRepetitions, result.getRepetitions());
            minIterations = Math.min(minIterations, result.getExecutionTimes());
         }
      }
      LOG.info("Repetitions: " + minRepetitions + " Iterations: " + minIterations);
      
      for (final Result result : chunk.getResult()) {
         if (!Double.isNaN(result.getValue()) && 
               result.getExecutionTimes() == minIterations &&
               result.getRepetitions() == minRepetitions) {
            if (result.getVersion().getGitversion().equals(versionPrevious)) {
               descPrev.addValue(result.getValue());
               previous.add(result);
            }
            if (result.getVersion().getGitversion().equals(versionCurrent)) {
               descCurrent.addValue(result.getValue());
               current.add(result);
            }
         }
         
      }
      LOG.trace("Built values: {} {}", previous.size(), current.size());
   }

   public void removeOutliers() {
      final OutlierRemover outlierRemover = new OutlierRemover(this);
      outlierRemover.remove();
   }

   public DescriptiveStatistics getDescPrevious() {
      return descPrev;
   }

   public DescriptiveStatistics getDescCurrent() {
      return descCurrent;
   }

   public List<Result> getPrevious() {
      return previous;
   }

   public List<Result> getCurrent() {
      return current;
   }

   public TestcaseStatistic getStatistic(final double type1error, final double type2error) {
//      final boolean isChange = StatisticUtil.agnosticTTest(descPrev, descCurrent, type1error, type2error) == de.peass.measurement.analysis.Relation.UNEQUAL;
      final boolean isChange = StatisticUtil.bimodalTTest(previous, current, type1error) != de.peass.measurement.analysis.Relation.EQUAL;
      final TestcaseStatistic statistic = new TestcaseStatistic(descPrev.getMean(), descCurrent.getMean(),
            descPrev.getStandardDeviation() / descPrev.getMean(), descCurrent.getStandardDeviation() / descCurrent.getMean(), descPrev.getN(),
            descPrev.getN() > 2 ? TestUtils.t(descPrev, descCurrent) : 0, isChange);
      statistic.setIsBimodal(StatisticUtil.isBimodal(previous, current));
      return statistic;
   }

   public double getDiff() {
      final double diff = (((getDescPrevious().getMean() - getDescCurrent().getMean()) * 10000) / getDescPrevious().getMean()) / 100;
      return diff;
   }
}
