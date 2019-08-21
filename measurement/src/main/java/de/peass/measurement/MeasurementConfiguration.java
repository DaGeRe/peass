package de.peass.measurement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class MeasurementConfiguration {
   private int timeout;
   private final int vms;
   private double type1error;
   private double type2error;

   public MeasurementConfiguration(final int vms) {
      this.timeout = 1000000;
      this.vms = vms;
      this.type1error = 0.01;
      this.type2error = 0.01;
   }

   @JsonCreator
   public MeasurementConfiguration(@JsonProperty("timeout") final int timeout,
         @JsonProperty("vms") final int vms,
         @JsonProperty("type1error") final double type1error,
         @JsonProperty("type2error") final double type2error) {
      this.timeout = timeout * 1000 * 60; // timeout in minutes, not in milliseconds
      this.vms = vms;
      this.type1error = type1error;
      this.type2error = type2error;
   }

   public int getTimeout() {
      return timeout;
   }

   public void setTimeout(final int timeout) {
      this.timeout = timeout;
   }

   public int getVms() {
      return vms;
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
}