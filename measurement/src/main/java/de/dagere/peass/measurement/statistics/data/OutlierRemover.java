package de.dagere.peass.measurement.statistics.data;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.kopemedata.VMResult;

public class OutlierRemover {

   public static final double Z_SCORE = 3.29;

   class RemoveInformation {
      int removedHigher = 0;
      int removedLower = 0;

      public int getSum() {
         return removedHigher + removedLower;
      }
   }

   private static final Logger LOG = LogManager.getLogger(OutlierRemover.class);

   private final DescribedChunk chunk;

   public OutlierRemover(DescribedChunk chunk) {
      this.chunk = chunk;
   }

   public void remove() {
      final RemoveInformation removePrevious = removeOutliers(chunk.getDescPrevious(), chunk.getPrevious());
      final RemoveInformation removeCurrent = removeOutliers(chunk.getDescCurrent(), chunk.getCurrent());
      if (removePrevious.getSum() < removeCurrent.getSum()) {
         removeFromPrevious(removePrevious, removeCurrent);
      } else if (removeCurrent.getSum() < removePrevious.getSum()) {
         removeFromCurrent(removePrevious, removeCurrent);
      }
   }

   private void removeFromPrevious(final RemoveInformation removePrevious, final RemoveInformation removeCurrent) {
      int additionalRemoves = removeCurrent.getSum() - removePrevious.getSum();
      while (additionalRemoves > 0) {
         if (removePrevious.removedHigher < removeCurrent.removedHigher) {
            removeByValue(chunk.getDescPrevious(), chunk.getPrevious(), chunk.getDescPrevious().getMax());
            removePrevious.removedHigher++;
         } else if (removePrevious.removedLower < removeCurrent.removedLower) {
            removeByValue(chunk.getDescPrevious(), chunk.getPrevious(), chunk.getDescPrevious().getMin());
            removePrevious.removedLower++;
         }
         additionalRemoves = removeCurrent.getSum() - removePrevious.getSum();
      }
   }
   
   private void removeFromCurrent(final RemoveInformation removePrevious, final RemoveInformation removeCurrent) {
      int additionalRemoves = removePrevious.getSum() - removeCurrent.getSum();
      while (additionalRemoves > 0) {
         if (removeCurrent.removedHigher < removePrevious.removedHigher) {
            removeByValue(chunk.getDescCurrent(), chunk.getCurrent(), chunk.getDescCurrent().getMax());
            removeCurrent.removedHigher++;
         } else if (removeCurrent.removedLower < removePrevious.removedLower) {
            removeByValue(chunk.getDescCurrent(), chunk.getCurrent(), chunk.getDescCurrent().getMin());
            removeCurrent.removedLower++;
         }
         additionalRemoves = removeCurrent.getSum() - removePrevious.getSum();
      }
   }

   private void removeByValue(DescriptiveStatistics statistics, List<VMResult> results, double value) {
      for (Iterator<VMResult> it = results.iterator(); it.hasNext();) {
         VMResult result = it.next();
         if (result.getValue() == value) {
            LOG.debug("Removing Value: {}", result.getValue());
            it.remove();
            break;
         }
      }
      rebuildStatistics(statistics, results);
   }


   private RemoveInformation removeOutliers(DescriptiveStatistics statistics, List<VMResult> results) {
      final RemoveInformation removals = new RemoveInformation();
      final double max = statistics.getMean() + Z_SCORE * statistics.getStandardDeviation();
      final double min = statistics.getMean() - Z_SCORE * statistics.getStandardDeviation();
      final int outliers = countOutliers(results, max, min);
      final double allowedOutliers = results.size() * 0.05;
      LOG.info("Outliers: {} Allowed: {} Values: {}", outliers, allowedOutliers, results.size());
      LOG.debug("Mean: {} Max: {} Min: {}", statistics.getMean(), max, min);
      if (outliers < allowedOutliers) {
         removeFromList(results, removals, max, min);
         rebuildStatistics(statistics, results);
      }
      return removals;
   }

   private void removeFromList(List<VMResult> results, RemoveInformation removals, final double max, final double min) {
      for (Iterator<VMResult> it = results.iterator(); it.hasNext();) {
         VMResult result = it.next();
         if (result.getValue() > max) {
            LOG.debug("Removing: {}", result.getValue());
            it.remove();
            removals.removedHigher++;
         }
         if (result.getValue() < min) {
            LOG.debug("Removing: {}", result.getValue());
            it.remove();
            removals.removedLower++;
         }
      }
   }

   private int countOutliers(List<VMResult> results, final double max, final double min) {
      int outliers = 0;
      for (VMResult result : results) {
         if (result.getValue() > max || result.getValue() < min) {
            outliers++;
         }
      }
      return outliers;
   }

   private void rebuildStatistics(DescriptiveStatistics statistics, List<VMResult> results) {
      statistics.clear();
      for (VMResult result : results) {
         statistics.addValue(result.getValue());
      }
   }
}
