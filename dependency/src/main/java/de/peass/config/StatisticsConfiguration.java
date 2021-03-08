package de.peass.config;

import java.io.Serializable;

public class StatisticsConfiguration implements Serializable {
   private static final long serialVersionUID = -6193031432004031500L;
   
   private double type1error = 0.01;
   private double type2error = 0.01;
   private double outlierFactor;
   private ImplementedTests statisticTest = ImplementedTests.AGNOSTIC_T_TEST;

   public double getType1error() {
      return type1error;
   }

   public void setType1error(final double type1error) {
      this.type1error = type1error;
   }

   public double getType2error() {
      return type2error;
   }

   public void setType2error(final double type2error) {
      this.type2error = type2error;
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
