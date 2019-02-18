package de.peran.analysis.helper.comparedata;

public class DecissionCount {
   private int measurements;
   private int bigDiff;
   private int tTestDiff;
   private int confidenceDiff;

   public int getMeasurements() {
      return measurements;
   }

   public void setMeasurements(int measurements) {
      this.measurements = measurements;
   }

   public int getBigDiff() {
      return bigDiff;
   }

   public void setBigDiff(int bigDiff) {
      this.bigDiff = bigDiff;
   }

   public int gettTestDiff() {
      return tTestDiff;
   }

   public void settTestDiff(int tTestDiff) {
      this.tTestDiff = tTestDiff;
   }

   public int getConfidenceDiff() {
      return confidenceDiff;
   }

   public void setConfidenceDiff(int confidenceDiff) {
      this.confidenceDiff = confidenceDiff;
   }
}