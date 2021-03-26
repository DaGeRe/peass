package de.peass.measurement.analysis;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.generated.Result;
import de.peass.measurement.analysis.statistics.EvaluationPair;
import de.peass.statistics.ConfidenceIntervalInterpretion;
import de.peass.statistics.StatisticUtil;
import de.peran.FolderSearcher;
import de.precision.analysis.repetitions.bimodal.CompareData;

public class TestStatistic {
   private static final Logger LOG = LogManager.getLogger(TestStatistic.class);

   private static final double CONFIDENCE = 0.02;

   private final int diff;
   private final double tValue;
   private final boolean change;
   private final Relation confidenceResult;

   private final DescriptiveStatistics statisticsPrevious;
   private final DescriptiveStatistics statisticsCurrent;

   public TestStatistic(final EvaluationPair data) {
      this(data, null);
   }

   public TestStatistic(final EvaluationPair data, final ProjectStatistics info) {
      List<Result> previous = data.getPrevius();
      List<Result> current = data.getCurrent();

      checkData(data, previous, current);

      previous = StatisticUtil.shortenValues(previous);
      current = StatisticUtil.shortenValues(current);

      CompareData cd = new CompareData(previous, current);
      confidenceResult = ConfidenceIntervalInterpretion.compare(cd);

      final int resultslength = Math.min(data.getCurrent().size(), data.getPrevius().size());

      LOG.trace("Results: {}", resultslength);

      statisticsPrevious = ConfidenceIntervalInterpretion.getStatistics(previous);
      statisticsCurrent = ConfidenceIntervalInterpretion.getStatistics(current);

      final List<Double> previous_double = MultipleVMTestUtil.getAverages(previous);
      final List<Double> after_double = MultipleVMTestUtil.getAverages(current);

      final double[] dataBefore = ArrayUtils.toPrimitive(previous_double.toArray(new Double[0]));
      final double[] dataAfter = ArrayUtils.toPrimitive(after_double.toArray(new Double[0]));
      final DescriptiveStatistics ds = new DescriptiveStatistics(dataBefore);
      final DescriptiveStatistics ds2 = new DescriptiveStatistics(dataAfter);
      LOG.trace(ds.getMean() + " " + ds2.getMean() + " " + ds.getStandardDeviation() + " " + ds2.getStandardDeviation());

      tValue = TestUtils.t(dataBefore, dataAfter);
      change = TestUtils.tTest(dataBefore, dataAfter, CONFIDENCE);

      // Achtung, dupliziert!
      diff = (int) (((statisticsPrevious.getMean() - statisticsCurrent.getMean()) * 10000) / statisticsPrevious.getMean());
      // double anovaDeviation = ANOVATestWrapper.getANOVADeviation(prevResults, currentResults);
      LOG.trace("Means: {} {} Diff: {} % T-Value: {} Change: {}", statisticsPrevious.getMean(), statisticsCurrent.getMean(), ((double) diff) / 100, tValue, change);

      addToInfo(data, info, resultslength);
   }

   private void addToInfo(final EvaluationPair data, final ProjectStatistics info, final int resultslength) {
      if (info != null) {
         info.addMeasurement(data.getVersion(), data.getTestcase(), statisticsPrevious, statisticsCurrent, resultslength);
         try {
            File resultFolder = new File("results");
            resultFolder.mkdir();
            FolderSearcher.MAPPER.writeValue(new File(resultFolder, "statistics.json"), info);
         } catch (final IOException e) {
            e.printStackTrace();
         }
      }
   }

   private void checkData(final EvaluationPair data, final List<Result> previous, final List<Result> current) {
      if (previous.size() == 0 || current.size() == 0) {
         LOG.error("Data empty: {} {}", data.getVersion());
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
}
