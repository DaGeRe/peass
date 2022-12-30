package de.dagere.peass.analysis.measurement;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.MannWhitneyUTest;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.config.StatisticsConfig;
import de.dagere.peass.measurement.dataloading.MultipleVMTestUtil;
import de.dagere.peass.measurement.dataloading.ResultLoader;
import de.dagere.peass.measurement.statistics.ConfidenceIntervalInterpretion;
import de.dagere.peass.measurement.statistics.Relation;
import de.dagere.peass.measurement.statistics.StatisticUtil;
import de.dagere.peass.measurement.statistics.bimodal.CompareData;
import de.dagere.peass.measurement.statistics.data.EvaluationPair;

public class TestStatistic {
   private static final Logger LOG = LogManager.getLogger(TestStatistic.class);

   private final int diff;
   private final double tValue;
   private final double mannWhitneyUStatistic;
   private final boolean change;
   private final Relation confidenceResult;

   private final DescriptiveStatistics statisticsPrevious;
   private final DescriptiveStatistics statisticsCurrent;

   public TestStatistic(final EvaluationPair data) {
      this(data, null, 0.01);
   }

   public TestStatistic(final EvaluationPair data, final ProjectStatistics info, final double type1error) {
      List<VMResult> previous = ResultLoader.removeResultsWithWrongConfiguration(data.getPrevius());
      List<VMResult> current = ResultLoader.removeResultsWithWrongConfiguration(data.getCurrent());

      checkData(data, previous, current);

      previous = StatisticUtil.shortenValues(previous);
      current = StatisticUtil.shortenValues(current);

      CompareData cd = new CompareData(previous, current);
      confidenceResult = ConfidenceIntervalInterpretion.compare(cd);

      final int resultslength = Math.min(data.getCurrent().size(), data.getPrevius().size());

      LOG.trace("Results: {}", resultslength);

      statisticsPrevious = ConfidenceIntervalInterpretion.getStatistics(previous);
      statisticsCurrent = ConfidenceIntervalInterpretion.getStatistics(current);

      final List<Double> predecessor_double = MultipleVMTestUtil.getAverages(previous);
      final List<Double> current_double = MultipleVMTestUtil.getAverages(current);

      final double[] dataPredecessor = ArrayUtils.toPrimitive(predecessor_double.toArray(new Double[0]));
      final double[] dataCurrent = ArrayUtils.toPrimitive(current_double.toArray(new Double[0]));
      final DescriptiveStatistics dsPredecessor = new DescriptiveStatistics(dataPredecessor);
      final DescriptiveStatistics dsCurrent = new DescriptiveStatistics(dataCurrent);
      LOG.trace(dsPredecessor.getMean() + " " + dsCurrent.getMean() + " " + dsPredecessor.getStandardDeviation() + " " + dsCurrent.getStandardDeviation());

      tValue = TestUtils.t(dataPredecessor, dataCurrent);
      mannWhitneyUStatistic = StatisticUtil.getMannWhitneyUStatistic(dataPredecessor, dataCurrent);
      change = TestUtils.homoscedasticTTest(dataPredecessor, dataCurrent, type1error);
      
      // Achtung, dupliziert!
      diff = (int) (((statisticsPrevious.getMean() - statisticsCurrent.getMean()) * 10000) / statisticsPrevious.getMean());
      // double anovaDeviation = ANOVATestWrapper.getANOVADeviation(prevResults, currentResults);
      LOG.trace("Means: {} {} Diff: {} % T-Value: {} Change: {}", statisticsPrevious.getMean(), statisticsCurrent.getMean(), ((double) diff) / 100, tValue, change);

      addToInfo(data, info, resultslength);
   }
   
   public TestStatistic(final EvaluationPair data, final ProjectStatistics info, final StatisticsConfig config) {
      this(data, info, config.getType1error());
   }

   private void addToInfo(final EvaluationPair data, final ProjectStatistics info, final int resultslength) {
      if (info != null) {
         info.addMeasurement(data.getCommit(), data.getTestcase(), statisticsPrevious, statisticsCurrent, resultslength);
      }
   }

   private void checkData(final EvaluationPair data, final List<VMResult> previous, final List<VMResult> current) {
      if (previous.size() == 0 || current.size() == 0) {
         LOG.error("Data empty: {} {}", data.getCommit());
         if (previous.size() == 0) {
            LOG.error("Previous  empty");
         }
         if (current.size() == 0) {
            LOG.error("Previous  empty");
         }
         throw new RuntimeException("Data of " + data.getTestcase() + " empty");
      }
      if (Double.isNaN(current.get(0).getDeviation()) || Double.isNaN(previous.get(0).getDeviation())) {
         throw new RuntimeException("Data contained NaN - not handling result");
      }
   }

   public boolean isChange() {
      return change;
   }

   public int getDiff() {
      return diff;
   }

   public double getTValue() {
      return tValue;
   }

   public Relation getConfidenceResult() {
      return confidenceResult;
   }

   public DescriptiveStatistics getPreviousStatistic() {
      return statisticsPrevious;
   }

   public DescriptiveStatistics getCurrentStatistic() {
      return statisticsCurrent;
   }

   @Override
   public String toString() {
      return tValue + " " + getStatisticString(statisticsPrevious) + " " + getStatisticString(statisticsCurrent);
   }

   private String getStatisticString(final DescriptiveStatistics statistics) {
      return statistics.getMean() + " " + (statistics.getStandardDeviation() / statistics.getMean());
   }

   public Double getMannWhitneyUStatistic() {
      return mannWhitneyUStatistic;
   }
}
