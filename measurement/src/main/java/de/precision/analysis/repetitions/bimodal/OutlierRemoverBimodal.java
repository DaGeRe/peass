package de.precision.analysis.repetitions.bimodal;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import de.dagere.kopeme.generated.Result;
import de.peass.measurement.analysis.MultipleVMTestUtil;
import de.peass.measurement.analysis.statistics.OutlierRemover;

public class OutlierRemoverBimodal {
   public OutlierRemoverBimodal(final List<Result> results) {
      final IsBimodal isBimodal = new IsBimodal(results);
      if (isBimodal.isBimodal()) {
         removeFromLeftDistribution(results, isBimodal);
         removeFromRightDistribution(results, isBimodal);
      } else {
         removeUnimodal(results);
      }
   }

   private void removeUnimodal(final List<Result> results) {
      SummaryStatistics statistics = MultipleVMTestUtil.getStatistic(results);
      for (Iterator<Result> iterator = results.iterator(); iterator.hasNext();) {
         Result r = iterator.next();
         double zscore = Math.abs(r.getValue() - statistics.getMean()) / statistics.getStandardDeviation();
         if (zscore > OutlierRemover.Z_SCORE) {
            iterator.remove();
         }
      }
   }

   private void removeFromRightDistribution(final List<Result> fastShortened, final IsBimodal isBimodal) {
      SummaryStatistics stat2 = isBimodal.getStat2();
      for (Iterator<Result> iterator = fastShortened.iterator(); iterator.hasNext();) {
         Result r = iterator.next();
         if (r.getValue() > isBimodal.getAvgValue()) {
            double zscore = Math.abs(r.getValue() - stat2.getMean()) / stat2.getStandardDeviation();
            if (zscore > OutlierRemover.Z_SCORE) {
               iterator.remove();
            }
         }
      }
   }

   private void removeFromLeftDistribution(final List<Result> fastShortened, final IsBimodal isBimodal) {
      SummaryStatistics stat1 = isBimodal.getStat1();
      for (Iterator<Result> iterator = fastShortened.iterator(); iterator.hasNext();) {
         Result r = iterator.next();
         if (r.getValue() < isBimodal.getAvgValue()) {
            double zscore = Math.abs(r.getValue() - stat1.getMean()) / stat1.getStandardDeviation();
            if (zscore > OutlierRemover.Z_SCORE) {
               iterator.remove();
            }
         }
      }
   }
}
