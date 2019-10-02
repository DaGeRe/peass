package de.peass.measurement.searchcause;

import de.peass.dependency.analysis.data.TestCase;

public class CauseSearcherConfig {
   private final TestCase testCase;
   private final boolean useAggregation;
   private final boolean saveAll;
   private final double outlierFactor;
   private final boolean splitAggregated;
   private final double minTime; 
   private final boolean calibrationRun;

   public CauseSearcherConfig(final TestCase testCase, final boolean useAggregation, final boolean saveAll, final double outlierFactor, 
         final boolean splitAggregated, final double minTime, final boolean calibrationRun) {
      this.testCase = testCase;
      this.useAggregation = useAggregation;
      this.saveAll = saveAll;
      this.outlierFactor = outlierFactor;
      this.splitAggregated = splitAggregated;
      this.minTime = minTime;
      this.calibrationRun = calibrationRun;
   }

   public TestCase getTestCase() {
      return testCase;
   }

   public boolean isUseAggregation() {
      return useAggregation;
   }
   
   public boolean isSaveAll() {
      return saveAll;
   }
   
   public double getOutlierFactor() {
      return outlierFactor;
   }
   
   public boolean isSplitAggregated() {
      return splitAggregated;
   }
   
   public double getMinTime() {
      return minTime;
   }

   public boolean useCalibrationRun() {
      return calibrationRun;
   }
}