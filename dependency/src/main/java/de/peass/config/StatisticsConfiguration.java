package de.peass.config;

public class StatisticsConfiguration {
   private double type1error = 0.01;
   private double type2error = 0.01;
   private double outlierFactor;
   private ImplementedTests statisticTest;

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
