package de.peran.measurement.analysis;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.inference.TestUtils;

public class Statistic {
   private double meanOld, meanCurrent;
   private double deviationOld, deviationCurrent;
   private long executions;
   private double tvalue;
   
   public Statistic() {
      
   }

   public Statistic(double mean1, double mean2, double deviation1, double deviation2, long executions, double tvalue) {
      super();
      this.meanOld = mean1;
      this.meanCurrent = mean2;
      this.deviationOld = deviation1;
      this.deviationCurrent = deviation2;
      this.executions = executions;
      this.tvalue = tvalue;
   }

   public double getMeanOld() {
      return meanOld;
   }

   public void setMeanOld(double mean1) {
      this.meanOld = mean1;
   }

   public double getMeanCurrent() {
      return meanCurrent;
   }

   public void setMeanCurrent(double mean2) {
      this.meanCurrent = mean2;
   }

   public double getDeviationOld() {
      return deviationOld;
   }

   public void setDeviationOld(double deviation1) {
      this.deviationOld = deviation1;
   }

   public double getDeviationCurrent() {
      return deviationCurrent;
   }

   public void setDeviationCurrent(double deviation2) {
      this.deviationCurrent = deviation2;
   }

   public long getExecutions() {
      return executions;
   }

   public void setExecutions(long executions) {
      this.executions = executions;
   }

   public double getTvalue() {
      return tvalue;
   }

   public void setTvalue(double tvalue) {
      this.tvalue = tvalue;
   }

   @Override
   public String toString() {
      return meanOld + " " + meanCurrent + " " + deviationOld / meanOld + " " + deviationCurrent / meanCurrent + " " + executions + " " + tvalue;
   }

   public static void main(String[] args) {
      double[] sample1 = new double[] { 1.0, 2.0, 3.0 };
      double[] sample2 = new double[] { 1.0, 2.0, 2.5 };

      DescriptiveStatistics stat1 = new DescriptiveStatistics(sample1);
      DescriptiveStatistics stat2 = new DescriptiveStatistics(sample2);

      System.out.println(TestUtils.tTest(sample1, sample2));
      System.out.println(TestUtils.t(sample1, sample2));
      System.out.println(TestUtils.tTest(sample1, sample2, 0.02));

      System.out.println(TestUtils.tTest(stat1, stat2));
      System.out.println(TestUtils.t(stat1, stat2));
      System.out.println(TestUtils.tTest(stat1, stat2, 0.02));

   }

}