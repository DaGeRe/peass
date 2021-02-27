package de.peass.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.peass.dependency.execution.MeasurementConfigurationMixin;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;

public class MeasurementConfiguration {

   public static final MeasurementConfiguration DEFAULT = new MeasurementConfiguration(300, 30, 0.01, 0.01);

   private final long timeout;
   private final int vms;
   private double type1error;
   private double type2error;
   private boolean earlyStop = true;
   private int warmup = 0;
   private int iterations = 1;
   private int repetitions = 1;
   private boolean logFullData = true;
   private boolean removeSnapshots = false;
   
   //Kieker config
   private boolean useKieker = false;
   private boolean useSourceInstrumentation = false;
   private boolean useSelectiveInstrumentation = false;
   private boolean useSampling = false;
   private boolean useCircularQueue = false;
   private boolean redirectToNull = true;
   private boolean enableAdaptiveMonitoring = false;
   private boolean useGC = true;
   private int kiekerAggregationInterval = 5000;
   private String javaVersion = System.getProperty("java.version");
   private AllowedKiekerRecord record = AllowedKiekerRecord.OPERATIONEXECUTION;
   private MeasurementStrategy measurementStrategy = MeasurementStrategy.SEQUENTIAL;

   private String version = "HEAD";
   private String versionOld = "HEAD~1";
   
   private ExecutionConfig executionConfig = new ExecutionConfig();

   public MeasurementConfiguration(final int vms) {
      this.timeout = 20 * 60 * 1000; // 20 minutes
      this.vms = vms;
      this.type1error = 0.01;
      this.type2error = 0.01;
   }

   public MeasurementConfiguration(final int vms, final long timeoutInMinutes) {
      this.timeout = timeoutInMinutes * 60 * 1000; // timeout in minutes is converted to milliseconds
      this.vms = vms;
      this.type1error = 0.01;
      this.type2error = 0.01;
   }

   public MeasurementConfiguration(final int vms, final String version, final String versionOld) {
      this.timeout = 20 * 60 * 1000; // 20 minutes
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

   public MeasurementConfiguration(final MeasurementConfigurationMixin mixin) {
      this(mixin.getTimeout(), mixin.getVms(), mixin.getType1error(), mixin.getType2error());
      setEarlyStop(mixin.isEarlyStop());
      setUseKieker(mixin.isUseKieker());
      setIterations(mixin.getIterations());
      setWarmup(mixin.getWarmup());
      setRepetitions(mixin.getRepetitions());
      setUseGC(mixin.isUseGC());
      setRecord(mixin.getRecord());
      setMeasurementStrategy(mixin.getMeasurementStrategy());
      executionConfig.setTestGoal(mixin.getTestGoal());
   }

   @JsonCreator
   public MeasurementConfiguration(@JsonProperty("timeout") final int timeout,
         @JsonProperty("vms") final int vms,
         @JsonProperty("type1error") final double type1error,
         @JsonProperty("type2error") final double type2error,
         @JsonProperty("earlystop") final boolean earlyStop,
         @JsonProperty("version") final String version,
         @JsonProperty("versionOld") final String versionOld) {
      this.timeout = timeout; // timeout in minutes is NOT converted, since it is the exact serialized timeout
      this.vms = vms;
      this.type1error = type1error;
      this.type2error = type2error;
      this.earlyStop = earlyStop;
      this.version = version;
      this.versionOld = versionOld;
   }
   
   /**
    * Copy constructor
    * 
    * @param other Configuration to copy
    */
   public MeasurementConfiguration(final MeasurementConfiguration other) {
      this.timeout = other.timeout;
      this.vms = other.vms;
      this.type1error = other.type1error;
      this.type2error = other.type2error;
      this.earlyStop = other.earlyStop;
      this.warmup = other.warmup;
      this.iterations = other.iterations;
      this.repetitions = other.repetitions;
      this.logFullData = other.logFullData;
      this.removeSnapshots = other.removeSnapshots;
      this.useKieker = other.useKieker;
      this.useSourceInstrumentation = other.useSourceInstrumentation;
      this.useSelectiveInstrumentation = other.useSelectiveInstrumentation;
      this.useSampling = other.useSampling;
      this.useCircularQueue = other.useCircularQueue;
      this.redirectToNull = other.redirectToNull;
      this.enableAdaptiveMonitoring = other.enableAdaptiveMonitoring;
      this.useGC = other.useGC;
      this.kiekerAggregationInterval = other.kiekerAggregationInterval;
      this.javaVersion = other.javaVersion;
      this.record = other.record;
      this.measurementStrategy = other.measurementStrategy;
      this.version = other.version;
      this.versionOld = other.versionOld;
   }

   /**
    * Whether to execute a GC before every iteration (bunch of repetitions)
    * 
    * @return
    */
   public boolean isUseGC() {
      return useGC;
   }

   public void setUseGC(final boolean useGC) {
      this.useGC = useGC;
   }

   public boolean isRedirectToNull() {
      return redirectToNull;
   }

   public void setRedirectToNull(final boolean redirectToNull) {
      this.redirectToNull = redirectToNull;
   }

   public long getTimeout() {
      return timeout;
   }

   @JsonIgnore
   public long getTimeoutInMinutes() {
      return timeout / 60 / 1000;
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

   public void setEarlyStop(final boolean earlyStop) {
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

   public void setLogFullData(final boolean logFullData) {
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

   public void setJavaVersion(final String javaVersion) {
      this.javaVersion = javaVersion;
   }

   public void setRecord(final AllowedKiekerRecord record) {
      if (record == null) {
         this.record = AllowedKiekerRecord.OPERATIONEXECUTION;
      } else {
         this.record = record;
      }
   }

   public AllowedKiekerRecord getRecord() {
      return record;
   }

   public boolean isUseSourceInstrumentation() {
      return useSourceInstrumentation;
   }

   public void setUseSourceInstrumentation(final boolean useSourceInstrumentation) {
      this.useSourceInstrumentation = useSourceInstrumentation;
   }

   public boolean isUseCircularQueue() {
      return useCircularQueue;
   }

   public void setUseCircularQueue(final boolean useCircularQueue) {
      this.useCircularQueue = useCircularQueue;
   }

   public boolean isUseSelectiveInstrumentation() {
      return useSelectiveInstrumentation;
   }

   public void setUseSelectiveInstrumentation(final boolean useSelectiveInstrumentation) {
      this.useSelectiveInstrumentation = useSelectiveInstrumentation;
   }

   public boolean isUseSampling() {
      return useSampling;
   }

   public void setUseSampling(final boolean useSampling) {
      this.useSampling = useSampling;
   }
   
   public boolean isEnableAdaptiveConfig() {
      return enableAdaptiveMonitoring;
   }
   
   public void setEnableAdaptiveConfig(final boolean allowAdaptiveConfig) {
      this.enableAdaptiveMonitoring = allowAdaptiveConfig;
   }

   public MeasurementStrategy getMeasurementStrategy() {
      return measurementStrategy;
   }

   public void setMeasurementStrategy(final MeasurementStrategy measurementStrategy) {
      this.measurementStrategy = measurementStrategy;
   }
   
   /**
    * Returns the warmup that should be ignored when individual nodes are measured 
    * @return
    */
   @JsonIgnore
   public int getNodeWarmup() {
      final int samplingfactor = this.isUseSampling() ? 1000 : 1;
      final int warmup = this.getWarmup() * this.getRepetitions() / samplingfactor;
      return warmup;
   }

   public void setRemoveSnapshots(final boolean removeSnapshots) {
      this.removeSnapshots = removeSnapshots;
   }
   
   public boolean isRemoveSnapshots() {
      return removeSnapshots;
   }
   
   public String getTestGoal() {
      return executionConfig.getTestGoal();
   }

   public void setTestGoal(final String testGoal) {
      executionConfig.setTestGoal(testGoal);
   }

   public List<String> getIncludes() {
      return executionConfig.getIncludes();
   }

   public void setIncludes(final List<String> includes) {
      executionConfig.setIncludes(includes);
   }
}