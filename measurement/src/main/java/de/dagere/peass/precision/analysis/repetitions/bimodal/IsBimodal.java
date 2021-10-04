package de.dagere.peass.precision.analysis.repetitions.bimodal;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.generated.Result;

class Division {

   private static final Logger LOG = LogManager.getLogger(Division.class);

   final SummaryStatistics statisticCandidate1 = new SummaryStatistics();
   final SummaryStatistics statisticCandidate2 = new SummaryStatistics();
   final int divisionIndex;
   double divisionMeanvalue;

   public Division(final double[] values, final int divisionIndex) {
      divisionMeanvalue = values[divisionIndex];
      this.divisionIndex = divisionIndex;
      for (double value : values) {
         if (value < divisionMeanvalue) {
            statisticCandidate1.addValue(value);
         } else {
            statisticCandidate2.addValue(value);
         }
      }
   }

   public double getSummaryVariance() {
      return statisticCandidate1.getVariance() + statisticCandidate2.getVariance();
   }

   static Division getOptimalDivision(final double[] values, final Division lastDivision, final int prevIndex) {
      int lastMiddleIndex = lastDivision.divisionIndex;
      int checkedDelta = Math.abs(prevIndex - lastMiddleIndex) / 2;

      if (checkedDelta == 0) {
         LOG.debug("Finally: " + lastDivision.divisionIndex + " " + lastDivision.divisionMeanvalue);
         return lastDivision;
      }

      Division leftPartDivision = new Division(values, lastMiddleIndex - checkedDelta);
      Division rightPartDivision = new Division(values, lastMiddleIndex + checkedDelta);

      Division result = null;

      LOG.trace("Checking left: {} {} Index: {}", leftPartDivision.getSummaryVariance(), lastDivision.getSummaryVariance(), leftPartDivision.divisionIndex);
      LOG.trace("Checking right: {} {} Index: {}", rightPartDivision.getSummaryVariance(), lastDivision.getSummaryVariance(), rightPartDivision.divisionIndex);
      if (leftPartDivision.getSummaryVariance() < rightPartDivision.getSummaryVariance()) {
         LOG.trace("Left smaller");
         if (leftPartDivision.getSummaryVariance() <= lastDivision.getSummaryVariance() && prevIndex != leftPartDivision.divisionIndex) {
            result = getOptimalDivision(values, leftPartDivision, lastMiddleIndex);
         } else {
            LOG.trace("Finally: " + lastDivision.divisionIndex + " " + lastDivision.divisionMeanvalue);
            result = lastDivision;
         }
      } else {
         LOG.trace("Right smaller");
         if (rightPartDivision.getSummaryVariance() <= lastDivision.getSummaryVariance() && prevIndex != rightPartDivision.divisionIndex) {
            result = getOptimalDivision(values, rightPartDivision, lastMiddleIndex);
         } else {
            LOG.trace("Finally: " + lastDivision.divisionIndex + " " + lastDivision.divisionMeanvalue);
            result = lastDivision;
         }
      }
      return result;
   }

   public double getMean() {
      return divisionMeanvalue;
   }

}

public class IsBimodal {

   private static final Logger LOG = LogManager.getLogger(IsBimodal.class);

   private final SummaryStatistics stat1;
   private final SummaryStatistics stat2;

   private final double avgValue, originalVariance;

   private final boolean isBimodal;

   public IsBimodal(final double[] values, final SummaryStatistics originalStat) {
      Arrays.sort(values);
      Division optimalDivision = Division.getOptimalDivision(values, new Division(values, values.length / 2), values.length);

      this.avgValue = optimalDivision.getMean();
      originalVariance = originalStat.getVariance();
      stat1 = optimalDivision.statisticCandidate1;
      stat2 = optimalDivision.statisticCandidate2;

      isBimodal = testBimodal();
   }

   public IsBimodal(final List<Result> fastShortened) {
      double[] values = new double[fastShortened.size()];
      SummaryStatistics originalStat = new SummaryStatistics();
      int i = 0;
      for (Result result : fastShortened) {
         values[i++] = result.getValue();
         originalStat.addValue(result.getValue());
      }

      Arrays.sort(values);
      Division optimalDivision = Division.getOptimalDivision(values, new Division(values, values.length / 2), values.length);

      this.avgValue = optimalDivision.getMean();
      originalVariance = originalStat.getVariance();
      stat1 = optimalDivision.statisticCandidate1;
      stat2 = optimalDivision.statisticCandidate2;

      isBimodal = testBimodal();
   }

   private boolean testBimodal() {
      LOG.trace("Deviations: " + stat1.getVariance() + " " + stat2.getVariance() + " (" + (stat1.getVariance() + stat2.getVariance()) + ") vs " + originalVariance);
      return stat1.getVariance() + stat2.getVariance() < originalVariance / 2 &&
            stat1.getN() > 2 && stat2.getN() > 2;
   }

   public double getAvgValue() {
      return avgValue;
   }

   public double getOriginalVariance() {
      return originalVariance;
   }

   public SummaryStatistics getStat1() {
      return stat1;
   }

   public SummaryStatistics getStat2() {
      return stat2;
   }

   public boolean isBimodal() {
      return isBimodal;
   }

}
