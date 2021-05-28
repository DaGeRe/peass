package de.dagere.peass.measurement.rca;

import java.io.File;

import picocli.CommandLine.Option;

public class CauseSearcherConfigMixin {

   @Option(names = { "-useCalibrationRun", "--useCalibrationRun" }, description = "Use the calibration run for complete measurements")
   private boolean useCalibrationRun = false;

   @Option(names = { "-useNonAggregatedWriter",
         "--useNonAggregatedWriter" }, description = "Whether to save non-aggregated JSON data for measurement results - if true, full kieker record data are stored")
   private boolean useNonAggregatedWriter = false;

   @Option(names = { "-useEOIs",
         "--useEOIs" }, description = "Use EOIs - nodes will be considered different if their kieker pattern or ess differ (needs space and computation time for big trees)")
   private boolean useEOIs = false;

   @Option(names = { "-notSplitAggregated", "--notSplitAggregated" }, description = "Whether to split the aggregated data (produces aggregated data per time slice)")
   private boolean notSplitAggregated = false;

   @Option(names = { "-propertyFolder", "--propertyFolder" }, description = "Path to property folder", required = false)
   protected File propertyFolder;

   @Option(names = { "-minTime", "--minTime" }, description = "Minimum node difference time compared to relative standard deviation. "
         + "If a node takes less time, its childs won't be measured (since time measurement isn't below accurate below a certain value).")
   private double minTime = 0.1;

   @Option(names = { "-rcaStrategy",
         "--rcaStrategy" }, description = "Strategy to select nodes which are measured (default: COMPLETE)")
   private RCAStrategy strategy = RCAStrategy.COMPLETE;

   @Option(names = { "-levels", "--levels" }, description = "Count of levels that should be measured at once; only allowed with strategy CONSTANT_LEVELS")
   private int levels = 1;

   public boolean isUseCalibrationRun() {
      return useCalibrationRun;
   }

   public boolean isUseNonAggregatedWriter() {
      return useNonAggregatedWriter;
   }

   public boolean isUseEOIs() {
      return useEOIs;
   }

   public boolean isNotSplitAggregated() {
      return notSplitAggregated;
   }

   public double getMinTime() {
      return minTime;
   }

   public RCAStrategy getStrategy() {
      return strategy;
   }

   public File getPropertyFolder() {
      return propertyFolder;
   }
   
   public int getLevels() {
      return levels;
   }

}