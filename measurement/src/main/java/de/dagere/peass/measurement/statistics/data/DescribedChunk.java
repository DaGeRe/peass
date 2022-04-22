package de.dagere.peass.measurement.statistics.data;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.kopeme.kopemedata.VMResultChunk;
import de.dagere.peass.config.StatisticsConfig;
import de.dagere.peass.measurement.dataloading.MultipleVMTestUtil;
import de.dagere.peass.measurement.statistics.StatisticUtil;

public class DescribedChunk {
   private static final Logger LOG = LogManager.getLogger(DescribedChunk.class);

   private final DescriptiveStatistics descPrev = new DescriptiveStatistics();
   private final DescriptiveStatistics descCurrent = new DescriptiveStatistics();

   private final List<VMResult> previous = new LinkedList<>();
   private final List<VMResult> current = new LinkedList<>();

   public DescribedChunk(final VMResultChunk chunk, final String versionPrevious, final String versionCurrent) {
      long minRepetitions = MultipleVMTestUtil.getMinRepetitionCount(chunk.getResults());
      long minIterations = MultipleVMTestUtil.getMinIterationCount(chunk.getResults());

      LOG.info("Repetitions: " + minRepetitions + " Iterations: " + minIterations);

      for (final VMResult result : chunk.getResults()) {
         if (!Double.isNaN(result.getValue()) &&
               result.getIterations() == minIterations &&
               result.getRepetitions() == minRepetitions) {
            if (result.getCommit().equals(versionPrevious)) {
               descPrev.addValue(result.getValue());
               previous.add(result);
            }
            if (result.getCommit().equals(versionCurrent)) {
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

   public List<VMResult> getPrevious() {
      return previous;
   }

   public List<VMResult> getCurrent() {
      return current;
   }

   public TestcaseStatistic getStatistic(StatisticsConfig config) {
      final boolean isChange = StatisticUtil.isDifferent(previous, current, config) != de.dagere.peass.measurement.statistics.Relation.EQUAL;
      TestcaseStatistic statistic = new TestcaseStatistic(descPrev, descCurrent, descPrev.getN(), descCurrent.getN());
      statistic.setChange(isChange);
      statistic.setIsBimodal(StatisticUtil.isBimodal(previous, current));
      return statistic;
   }

   public double getDiff() {
      final double diff = (((getDescPrevious().getMean() - getDescCurrent().getMean()) * 10000) / getDescPrevious().getMean()) / 100;
      return diff;
   }
}
