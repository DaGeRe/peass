package de.dagere.peass.config;

import java.io.Serializable;

import de.dagere.peass.config.parameters.StatisticsConfigMixin;

public class StatisticsConfig implements Serializable {
   private static final long serialVersionUID = -6193031432004031500L;

   /**
    * Type 1 error is the false positive rate (=1-significance) for regular two-sided t-test
    */
   private double type1error = StatisticsConfigMixin.PEASS_DEFAULT_TYPE_1_ERROR;
   /**
    * Type 2 error is the false negative rate for agnostic t-test
    */
   private double type2error = StatisticsConfigMixin.PEASS_DEFAULT_TYPE_2_ERROR;
   private double outlierFactor = DEFAULT_OUTLIER_FACTOR;
   private StatisticalTests statisticTest = StatisticalTests.T_TEST;
   
   public static final double DEFAULT_OUTLIER_FACTOR = 3.29; // Does not remove 99% of all values in gaussian distribution

   public StatisticsConfig() {
   }
   
   public StatisticsConfig(final StatisticsConfig other) {
      this.type1error = other.type1error;
      this.type2error = other.type2error;
      this.outlierFactor = other.outlierFactor;
      this.statisticTest = other.statisticTest;
   }

   public double getType1error() {
      return type1error;
   }

   public void setType1error(final double type1error) {
      this.type1error = type1error;
      if (type1error <= 0.0 || type1error >= 1.0) {
         throw new RuntimeException("Configured illegal type1error: " + type1error);
      }
   }

   public double getType2error() {
      return type2error;
   }

   public void setType2error(final double type2error) {
      this.type2error = type2error;
      if (type2error <= 0.0 || type2error >= 1.0) {
         throw new RuntimeException("Configured illegal type1error: " + type1error);
      }
   }

   public double getOutlierFactor() {
      return outlierFactor;
   }

   public void setOutlierFactor(final double outlierFactor) {
      this.outlierFactor = outlierFactor;
   }

   public StatisticalTests getStatisticTest() {
      return statisticTest;
   }

   public void setStatisticTest(final StatisticalTests statisticTest) {
      this.statisticTest = statisticTest;
   }

}
