package de.peass.dependency.execution;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;

import de.peass.utils.Constants;

public class MeasurementConfiguration {

   public static final MeasurementConfiguration DEFAULT = new MeasurementConfiguration(300, 30, 0.01, 0.01);

   private int timeout;
   private final int vms;
   private double type1error;
   private double type2error;
   private boolean earlyStop = true;
   private int warmup = 0;
   private int iterations = 1;
   private int repetitions = 1;
   private boolean logFullData = true;
   private boolean useKieker = false;
   private boolean useGC = true;
   private int kiekerAggregationInterval = 5000;
   private String javaVersion = System.getProperty("java.version");

   private String version;
   private String versionOld;

   public MeasurementConfiguration(final int vms) {
      this.timeout = 1000000;
      this.vms = vms;
      this.type1error = 0.01;
      this.type2error = 0.01;
   }

   public MeasurementConfiguration(final int vms, final String version, final String versionOld) {
      this.timeout = 1000000;
      this.vms = vms;
      this.type1error = 0.01;
      this.type2error = 0.01;
      this.version = version;
      this.versionOld = versionOld;
   }

   public MeasurementConfiguration(@JsonProperty("timeout") final int timeout,
         @JsonProperty("vms") final int vms,
         @JsonProperty("type1error") final double type1error,
         @JsonProperty("type2error") final double type2error) {
      this.timeout = timeout * 1000 * 60; // timeout in minutes is converted to milliseconds
      this.vms = vms;
      this.type1error = type1error;
      this.type2error = type2error;
   }

   @JsonCreator
   public MeasurementConfiguration(@JsonProperty("timeout") final int timeout,
         @JsonProperty("vms") final int vms,
         @JsonProperty("type1error") final double type1error,
         @JsonProperty("type2error") final double type2error,
         @JsonProperty("earlystop") final boolean earlyStop,
         @JsonProperty("version") final String version,
         @JsonProperty("versionOld") final String versionOld) {
      this.timeout = timeout; // timeout in minutes, not in milliseconds
      this.vms = vms;
      this.type1error = type1error;
      this.type2error = type2error;
      this.earlyStop = earlyStop;
      this.version = version;
      this.versionOld = versionOld;
   }

   /**
    * Whether to execute a GC before every iteration (bunch of repetitions)
    * 
    * @return
    */
   public boolean isUseGC() {
      return useGC;
   }

   public void setUseGC(boolean useGC) {
      this.useGC = useGC;
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

   public boolean isEarlyStop() {
      return earlyStop;
   }

   public void setEarlyStop(boolean earlyStop) {
      this.earlyStop = earlyStop;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(final String version) {
      this.version = version;
   }

   public String getVersionOld() {
      return versionOld;
   }

   public void setVersionOld(final String versionOld) {
      this.versionOld = versionOld;
   }

   public int getWarmup() {
      return warmup;
   }

   public void setWarmup(final int warmup) {
      this.warmup = warmup;
   }

   public int getIterations() {
      return iterations;
   }

   public void setIterations(final int iterations) {
      this.iterations = iterations;
   }

   public int getRepetitions() {
      return repetitions;
   }

   public void setRepetitions(final int repetitions) {
      this.repetitions = repetitions;
   }

   public boolean isLogFullData() {
      return logFullData;
   }

   public void setLogFullData(boolean logFullData) {
      this.logFullData = logFullData;
   }

   public boolean isUseKieker() {
      return useKieker;
   }

   public void setUseKieker(final boolean useKieker) {
      this.useKieker = useKieker;
   }

   public int getKiekerAggregationInterval() {
      return kiekerAggregationInterval;
   }

   public void setKiekerAggregationInterval(final int kiekerAggregationInterval) {
      this.kiekerAggregationInterval = kiekerAggregationInterval;
   }

   public String getJavaVersion() {
      return javaVersion;
   }

   public void setJavaVersion(String javaVersion) {
      this.javaVersion = javaVersion;
   }
}