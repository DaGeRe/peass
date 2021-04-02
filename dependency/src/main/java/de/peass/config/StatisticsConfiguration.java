package de.peass.config;

import java.io.Serializable;

public class StatisticsConfiguration implements Serializable {
   private static final long serialVersionUID = -6193031432004031500L;

   private double type1error = 0.01;
   private double type2error = 0.01;
   private double outlierFactor = DEFAULT_OUTLIER_FACTOR;
   private ImplementedTests statisticTest = ImplementedTests.BIMODAL_T_TEST;
   
   public static final double DEFAULT_OUTLIER_FACTOR = 3.29; // Does not remove 99% of all values in gaussian distribution

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

   public ImplementedTests getStatisticTest() {
      return statisticTest;
   }

   public void setStatisticTest(final ImplementedTests statisticTest) {
      this.statisticTest = statisticTest;
   }

}
