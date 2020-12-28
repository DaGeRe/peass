package de.peass.measurement.rca;

import java.io.File;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.peass.dependency.analysis.data.TestCase;

public class CauseSearcherConfig {
   
   private final TestCase testCase;
   private final boolean ignoreEOIs;
   private final boolean useAggregation;
   private final boolean saveAll;
   private final double outlierFactor;
   private final boolean splitAggregated;
   private final double minTime;
   private final boolean calibrationRun;
   
   @JsonInclude(Include.NON_NULL)
   private File propertyFolder;
   private final RCAStrategy rcaStrategy;

   @JsonCreator
   public CauseSearcherConfig(@JsonProperty("testcase") final TestCase testCase,
         @JsonProperty("useAggregation") final boolean useAggregation,
         @JsonProperty("saveAll") final boolean saveAll,
         @JsonProperty("outlierFactor") final double outlierFactor,
         @JsonProperty("splitAggregated") final boolean splitAggregated,
         @JsonProperty("minTime") final double minTime,
         @JsonProperty("calibrationRun") final boolean calibrationRun,
         @JsonProperty("ignoreEOIs") final boolean ignoreEOIs,
         @JsonProperty("rcaStrategy") final RCAStrategy rcaStrategy) {
      this.testCase = testCase;
      this.useAggregation = useAggregation;
      this.saveAll = saveAll;
      this.outlierFactor = outlierFactor;
      this.splitAggregated = splitAggregated;
      this.minTime = minTime;
      this.calibrationRun = calibrationRun;
      this.ignoreEOIs = ignoreEOIs;
      this.rcaStrategy = rcaStrategy;
      propertyFolder = null;
      if (useAggregation && !ignoreEOIs) {
         throw new RuntimeException("EOIs need always to be ignored if aggregation is enabled!");
      }
   }
   
   public CauseSearcherConfig(TestCase test, CauseSearcherConfigMixin config) {
      this(test, !config.isUseNonAggregatedWriter(), !config.isSaveNothing(),
            config.getOutlierFactor(), !config.isNotSplitAggregated(), config.getMinTime(), config.isUseCalibrationRun(), !config.isUseEOIs(), 
            config.getStrategy());
      this.propertyFolder = config.getPropertyFolder();
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
   
   public boolean isIgnoreEOIs() {
      return ignoreEOIs;
   }
   
   public RCAStrategy getRcaStrategy() {
      return rcaStrategy;
   }
   
   public File getPropertyFolder() {
      return propertyFolder;
   }
   
}