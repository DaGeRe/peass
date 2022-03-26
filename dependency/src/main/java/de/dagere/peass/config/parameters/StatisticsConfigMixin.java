package de.dagere.peass.config.parameters;

import de.dagere.peass.config.StatisticalTests;
import de.dagere.peass.config.StatisticsConfig;
import picocli.CommandLine.Option;

public class StatisticsConfigMixin {
   
   public static final double PEASS_DEFAULT_TYPE_1_ERROR = 0.01;
   public static final double PEASS_DEFAULT_TYPE_2_ERROR = 0.01;
   
   @Option(names = { "-outlierFactor", "--outlierFactor" }, description = "Whether outliers should be removed with z-score higher than the given value")
   private double outlierFactor = 3.29;

   @Option(names = { "-type1error",
         "--type1error" }, description = "Type 1 error of t-test/false positive rate, i.e. probability of considering measurements unequal when they are equal")
   public double type1error = PEASS_DEFAULT_TYPE_1_ERROR;

   @Option(names = { "-type2error",
         "--type2error" }, description = "Type 2 error of *agnostic* t-test/false negative rate, i.e. probability of considering measurements equal when they are unequal")
   protected double type2error = PEASS_DEFAULT_TYPE_2_ERROR;
   
   @Option(names = { "-statisticTest", "--statisticTest" }, description = "Statistic test to use for comparison, default t-test", required = false)
   private StatisticalTests statisticTest = StatisticalTests.T_TEST;

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

   public StatisticalTests getStatisticTest() {
      return statisticTest;
   }

   public void setStatisticTest(final StatisticalTests statisticTest) {
      this.statisticTest = statisticTest;
   }
   
   public StatisticsConfig getStasticsConfig() {
      StatisticsConfig config = new StatisticsConfig();
      config.setType1error(getType1error());
      config.setType2error(getType2error());
      config.setStatisticTest(getStatisticTest());
      config.setOutlierFactor(getOutlierFactor());
      return config;
   }
   
   
}
