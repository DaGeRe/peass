package de.dagere.peass.analysis.changes;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Saves information about one change
 * 
 * @author reichelt
 *
 */
public class Change implements Serializable {
   
   private static final long serialVersionUID = -3691925375556886338L;

   private String diff;
   private String method;
   private String params;
   private double oldTime;
   private double changePercent;
   private double tvalue;
   private long vms;
   
   public Change() {
   }
   
   public Change(final String diff, final String method) {
      this.diff = diff;
      this.method = method;
   }
   
   public long getVms() {
      return vms;
   }

   public void setVms(final long vms) {
      this.vms = vms;
   }
   
   @JsonInclude(Include.NON_EMPTY)
   public String getParams() {
      return params;
   }
   
   public void setParams(final String params) {
      this.params = params;
   }
   
   @JsonIgnore
   public String getMethodWithParams() {
      if (params == null) {
         return method;
      } else {
         return method + "(" + params + ")";
      }
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