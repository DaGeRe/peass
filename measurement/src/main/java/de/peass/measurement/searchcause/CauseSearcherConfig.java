package de.peass.measurement.searchcause;

import de.peass.dependency.analysis.data.TestCase;

public class CauseSearcherConfig {
   private final TestCase testCase;
   private final boolean useAggregation;
   private final boolean saveAll;
   private final double outlierFactor;

   public CauseSearcherConfig(final TestCase testCase, final boolean useAggregation, final boolean saveAll, final double outlierFactor) {
      this.testCase = testCase;
      this.useAggregation = useAggregation;
      this.saveAll = saveAll;
      this.outlierFactor = outlierFactor;
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
}