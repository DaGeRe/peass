package de.dagere.peass.measurement.statistics.bimodal;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.kopemedata.VMResult;

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

   public IsBimodal(final List<VMResult> fastShortened) {
      double[] values = new double[fastShortened.size()];
      SummaryStatistics originalStat = new SummaryStatistics();
      int i = 0;
      for (VMResult result : fastShortened) {
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
