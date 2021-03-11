package de.peass.config;

import picocli.CommandLine.Option;

public class StatisticsConfigurationMixin {
   @Option(names = { "-outlierFactor", "--outlierFactor" }, description = "Whether outliers should be removed with z-score higher than the given value")
   private double outlierFactor = 5.0;

   @Option(names = { "-type1error",
         "--type1error" }, description = "Type 1 error of agnostic-t-test, i.e. probability of considering measurements equal when they are unequal (requires earlyStop)")
   public double type1error = 0.05;

   @Option(names = { "-type2error",
         "--type2error" }, description = "Type 2 error of agnostic-t-test, i.e. probability of considering measurements unequal when they are equal (requires earlyStop)")
   protected double type2error = 0.01;
   
   @Option(names = { "-statisticTest", "--statisticTest" }, description = "Statistic test to use for comparison, default agnostic t test", required = false)
   private ImplementedTests statisticTest = ImplementedTests.AGNOSTIC_T_TEST;

   public double getOutlierFactor() {
      return outlierFactor;
   }

   public void setOutlierFactor(final double outlierFactor) {
      this.outlierFactor = outlierFactor;
   }

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

   public ImplementedTests getStatisticTest() {
      return statisticTest;
   }

   public void setStatisticTest(final ImplementedTests statisticTest) {
      this.statisticTest = statisticTest;
   }
   
   
}
