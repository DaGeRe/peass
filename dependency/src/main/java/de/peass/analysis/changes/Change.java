package de.peass.analysis.changes;

/**
 * Saves information about one change
 * 
 * @author reichelt
 *
 */
public class Change {
   private String diff;
   private String method;
   private double oldTime;
   private double changePercent;
   private double tvalue;
   private long vms;
   
   public Change() {
   }
   
   public Change(String diff, String method) {
      this.diff = diff;
      this.method = method;
   }
   
   public long getVms() {
      return vms;
   }

   public void setVms(long vms) {
      this.vms = vms;
   }

   public String getDiff() {
      return diff;
   }

   public void setDiff(final String diff) {
      this.diff = diff;
   }

   public double getChangePercent() {
      return changePercent;
   }

   public void setChangePercent(final double changePercent) {
      this.changePercent = changePercent;
   }

   public double getTvalue() {
      return tvalue;
   }

   public void setTvalue(final double tvalue) {
      this.tvalue = tvalue;
   }

   public String getMethod() {
      return method;
   }

   public void setMethod(final String method) {
      this.method = method;
   }

   public void setOldTime(final double oldTime) {
      this.oldTime = oldTime;
   }

   public double getOldTime() {
      return oldTime;
   }

}