package de.peass.measurement.analysis.statistics;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.stat.inference.TestUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class TestcaseStatistic {
   private double meanOld, meanCurrent;

   /** Absolute (!) deviation of measured values **/
   private double deviationOld, deviationCurrent;
   private long vms;
   private long callsOld, calls;
   private double tvalue;
   @JsonInclude(Include.NON_NULL)
   private String predecessor;
   private Boolean isChange, isBimodal;

   public TestcaseStatistic() {

   }

   public TestcaseStatistic(final StatisticalSummary statisticsOld, final StatisticalSummary statisticsCurrent, final long callsOld, final long calls) {
      boolean oldHasValues = (statisticsOld != null && statisticsOld.getN() > 0);
      boolean currentHasValues = (statisticsCurrent != null && statisticsCurrent.getN() > 0);
      this.meanCurrent = currentHasValues ? statisticsCurrent.getMean() : Double.NaN;
      this.meanOld = oldHasValues ? statisticsOld.getMean() : Double.NaN;
      this.deviationCurrent = currentHasValues ? statisticsCurrent.getStandardDeviation() : Double.NaN;
      this.deviationOld = oldHasValues ? statisticsOld.getStandardDeviation() : Double.NaN;
      if (currentHasValues && oldHasValues) {
         this.vms = (statisticsCurrent.getN() + statisticsOld.getN()) / 2;
      } else if (oldHasValues) {
         this.vms = statisticsOld.getN();
      } else if (currentHasValues) {
         this.vms = statisticsCurrent.getN();
      } else {
         vms = 0;
      }
      this.tvalue = (oldHasValues && currentHasValues) ? TestUtils.t(statisticsOld, statisticsCurrent) : -1;
      this.isChange = null;
      this.calls = calls;
      this.callsOld = callsOld;

      check();
   }

   private void check() {
      if (callsOld == 0 && (!Double.isNaN(meanOld) || !Double.isNaN(deviationOld))) {
         throw new RuntimeException("Old data need to be not defined at all or contain a count of calls, a mean and a deviation!");
      }

      if (calls == 0 && (!Double.isNaN(meanCurrent) || !Double.isNaN(deviationCurrent))) {
         throw new RuntimeException("Current data need to be not defined at all or contain a count of calls, a mean and a deviation!");
      }
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

   public long getCallsOld() {
      return callsOld;
   }

   public void setCallsOld(final long callsOld) {
      this.callsOld = callsOld;
   }

   public long getCalls() {
      return calls;
   }

   public void setCalls(final long calls) {
      this.calls = calls;
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

   @JsonInclude(Include.NON_NULL)
   public Boolean isChange() {
      return isChange;
   }

   public void setChange(final Boolean isChange) {
      this.isChange = isChange;
   }

   @JsonInclude(Include.NON_NULL)
   public Boolean getIsBimodal() {
      return isBimodal;
   }

   public void setIsBimodal(Boolean isBimodal) {
      this.isBimodal = isBimodal;
   }

   @JsonIgnore
   public StatisticalSummary getStatisticsCurrent() {
      return new StatisticalSummaryValues(meanCurrent, deviationCurrent * deviationCurrent, vms, Double.MAX_VALUE, 0, meanCurrent * vms);
   }

   @JsonIgnore
   public StatisticalSummary getStatisticsOld() {
      return new StatisticalSummaryValues(meanOld, deviationOld * deviationOld, vms, Double.MAX_VALUE, 0, meanOld * vms);
   }

}