package de.peass.measurement.analysis.statistics;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class TestcaseStatistic {
   private double meanOld, meanCurrent;
   private double deviationOld, deviationCurrent;
   private long vms;
   private double tvalue;
   @JsonInclude(Include.NON_NULL)
   private String predecessor;
   private boolean isChange;

   public TestcaseStatistic() {

   }

   public TestcaseStatistic(final SummaryStatistics statisticsCurrent, final SummaryStatistics statisticsOld) {
      this.meanCurrent = statisticsCurrent.getMean();
      this.meanOld = statisticsOld.getMean();
      this.deviationCurrent = statisticsCurrent.getStandardDeviation();
      this.deviationOld = statisticsOld.getStandardDeviation();
      this.vms = statisticsCurrent.getN() + statisticsOld.getN();
      this.tvalue = TestUtils.t(statisticsOld, statisticsCurrent);
      this.isChange = Math.abs(tvalue) > 3.2;
   }

   public TestcaseStatistic(final double mean1, final double mean2, final double deviation1, final double deviation2, final long executions, final double tvalue,
         final boolean isChange) {
      super();
      this.meanOld = mean1;
      this.meanCurrent = mean2;
      this.deviationOld = deviation1;
      this.deviationCurrent = deviation2;
      this.vms = executions;
      this.tvalue = tvalue;
      this.setChange(isChange);
   }
   
   public String getPredecessor() {
      return predecessor;
   }

   public void setPredecessor(final String predecessor) {
      this.predecessor = predecessor;
   }

   public double getMeanOld() {
      return meanOld;
   }

   public void setMeanOld(final double mean1) {
      this.meanOld = mean1;
   }

   public double getMeanCurrent() {
      return meanCurrent;
   }

   public void setMeanCurrent(final double mean2) {
      this.meanCurrent = mean2;
   }

   public double getDeviationOld() {
      return deviationOld;
   }

   public void setDeviationOld(final double deviation1) {
      this.deviationOld = deviation1;
   }

   public double getDeviationCurrent() {
      return deviationCurrent;
   }

   public void setDeviationCurrent(final double deviation2) {
      this.deviationCurrent = deviation2;
   }

   public long getVMs() {
      return vms;
   }

   public void setVMs(final long vms) {
      this.vms = vms;
   }

   public double getTvalue() {
      return tvalue;
   }

   public void setTvalue(final double tvalue) {
      this.tvalue = tvalue;
   }

   @Override
   public String toString() {
      return meanOld + " " + meanCurrent + " " + deviationOld / meanOld + " " + deviationCurrent / meanCurrent + " " + vms + " " + tvalue;
   }

   public static void main(final String[] args) {
      final double[] sample1 = new double[] { 1.0, 2.0, 3.0 };
      final double[] sample2 = new double[] { 1.0, 2.0, 2.5 };

      final DescriptiveStatistics stat1 = new DescriptiveStatistics(sample1);
      final DescriptiveStatistics stat2 = new DescriptiveStatistics(sample2);

      System.out.println(TestUtils.tTest(sample1, sample2));
      System.out.println(TestUtils.t(sample1, sample2));
      System.out.println(TestUtils.tTest(sample1, sample2, 0.02));

      System.out.println(TestUtils.tTest(stat1, stat2));
      System.out.println(TestUtils.t(stat1, stat2));
      System.out.println(TestUtils.tTest(stat1, stat2, 0.02));

   }

   public boolean isChange() {
      return isChange;
   }

   public void setChange(final boolean isChange) {
      this.isChange = isChange;
   }
   
   @JsonIgnore
   public StatisticalSummary getStatisticsCurrent() {
      return new StatisticalSummaryValues(meanCurrent, deviationCurrent*deviationCurrent, vms/2, Double.MAX_VALUE, 0, meanCurrent*vms);
   }
   
   @JsonIgnore
   public StatisticalSummary getStatisticsOld() {
      return new StatisticalSummaryValues(meanOld, deviationOld*deviationOld, vms/2, Double.MAX_VALUE, 0, meanOld*vms);
   }

}