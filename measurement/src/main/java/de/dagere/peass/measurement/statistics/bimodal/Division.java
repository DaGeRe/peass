package de.dagere.peass.measurement.statistics.bimodal;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
         LOG.trace("Finally: " + lastDivision.divisionIndex + " " + lastDivision.divisionMeanvalue);
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