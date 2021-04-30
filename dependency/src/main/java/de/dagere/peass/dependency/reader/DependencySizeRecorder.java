package de.dagere.peass.dependency.reader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DependencySizeRecorder {
   
   private static final Logger LOG = LogManager.getLogger(DependencySizeRecorder.class);
   
   private int overallSize = 0, prunedSize = 0;

   public void setPrunedSize(final int size) {
      prunedSize = size;
   }
   
   public void addVersionSize(final int overallSizeAddition, final int prunedSizeAddition) {
      overallSize += overallSizeAddition;
      prunedSize += prunedSizeAddition;

      LOG.info("Overall-tests: {} Executed tests with pruning: {}", overallSize, prunedSize);

   }
}
