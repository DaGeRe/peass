package de.dagere.peass.config;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.dagere.peass.config.parameters.ExecutionConfigMixin;
import de.dagere.peass.config.parameters.KiekerConfigMixin;
import de.dagere.peass.config.parameters.MeasurementConfigurationMixin;
import de.dagere.peass.config.parameters.StatisticsConfigMixin;

public class MeasurementConfig implements Serializable {

   private static final long serialVersionUID = -6936740902708676182L;

   public static final MeasurementConfig DEFAULT = new MeasurementConfig(30);

   private final int vms;
   private boolean earlyStop = true;
   private int warmup = 0;
   private int iterations = 1;
   private int repetitions = 1;
   private boolean logFullData = true;
   private boolean useGC = false;

   private boolean callSyncBetweenVMs = true;
   private int waitTimeBetweenVMs = 1000;

   private final KiekerConfig kiekerConfig;

   @JsonInclude(JsonInclude.Include.NON_DEFAULT)
   private boolean saveAll = true;

   private String javaVersion = System.getProperty("java.version");

   private MeasurementStrategy measurementStrategy = MeasurementStrategy.SEQUENTIAL;

   private StatisticsConfig statisticsConfig = new StatisticsConfig();
   private final ExecutionConfig executionConfig;

   public MeasurementConfig(final int vms) {
      executionConfig = new ExecutionConfig(20);
      kiekerConfig = new KiekerConfig();
      this.vms = vms;
   }

   public MeasurementConfig(final int vms, final ExecutionConfig executionConfig, final KiekerConfig kiekerConfig) {
      this.executionConfig = executionConfig;
      this.vms = vms;
      this.kiekerConfig = new KiekerConfig(kiekerConfig);
   }
   
   public MeasurementConfig(final int vms, final String version, final String versionOld) {
      executionConfig = new ExecutionConfig(20);
      kiekerConfig = new KiekerConfig();
      this.vms = vms;
      executionConfig.setVersion(version);
      executionConfig.setVersionOld(versionOld);
   }

   public MeasurementConfig(final MeasurementConfigurationMixin mixin, final ExecutionConfigMixin executionMixin, 
         final StatisticsConfigMixin statisticMixin, final KiekerConfigMixin kiekerConfigMixin) {
      executionConfig = executionMixin.getExecutionConfig();
      kiekerConfig = kiekerConfigMixin.getKiekerConfig();
      kiekerConfig.setRecord(mixin.getRecord());
      statisticsConfig = statisticMixin.getStasticsConfig();
      this.vms = mixin.getVms();
      setEarlyStop(mixin.isEarlyStop());
      setUseKieker(mixin.isUseKieker());
      setIterations(mixin.getIterations());
      setWarmup(mixin.getWarmup());
      setRepetitions(mixin.getRepetitions());
      setUseGC(mixin.isUseGC());
      setMeasurementStrategy(mixin.getMeasurementStrategy());
      setShowStart(mixin.isShowStart());

      saveAll = !mixin.isSaveNothing();
   }

   @JsonCreator
   public MeasurementConfig(@JsonProperty("vms") final int vms,
         @JsonProperty("earlystop") final boolean earlyStop) {
      executionConfig = new ExecutionConfig();
      kiekerConfig = new KiekerConfig();
      this.vms = vms;
      this.earlyStop = earlyStop;
   }
   
   public MeasurementConfig(final int timeout, final int vms, final boolean earlyStop, final String version, final String versionOld) {
      executionConfig = new ExecutionConfig();
      executionConfig.setTimeout(timeout);
      executionConfig.setVersion(version);
      executionConfig.setVersionOld(versionOld);
      kiekerConfig = new KiekerConfig();
      this.vms = vms;
      this.earlyStop = earlyStop;
   }

   /**
    * Copy constructor
    * 
    * @param other Configuration to copy
    */
   public MeasurementConfig(final MeasurementConfig other) {
      executionConfig = new ExecutionConfig(other.getExecutionConfig());
      statisticsConfig = new StatisticsConfig(other.getStatisticsConfig());
      kiekerConfig = new KiekerConfig(other.getKiekerConfig());
      this.vms = other.vms;
      this.earlyStop = other.earlyStop;
      this.warmup = other.warmup;
      this.iterations = other.iterations;
      this.repetitions = other.repetitions;
      this.logFullData = other.logFullData;
      this.useGC = other.useGC;
      this.javaVersion = other.javaVersion;
      this.measurementStrategy = other.measurementStrategy;
      this.saveAll = other.saveAll;
      this.waitTimeBetweenVMs = other.waitTimeBetweenVMs;
   }

   public StatisticsConfig getStatisticsConfig() {
      return statisticsConfig;
   }

   public void setStatisticsConfig(final StatisticsConfig statisticsConfig) {
      this.statisticsConfig = statisticsConfig;
   }

   public void setSaveAll(final boolean saveAll) {
      this.saveAll = saveAll;
   }

   public boolean isSaveAll() {
      return saveAll;
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

   @JsonIgnore
   public long getTimeoutInSeconds() {
      return executionConfig.getTimeoutInSeconds();
   }

   public int getVms() {
      return vms;
   }

   public boolean isEarlyStop() {
      return earlyStop;
   }

   public void setEarlyStop(final boolean earlyStop) {
      this.earlyStop = earlyStop;
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

   /**
    * Warmup could be considered by the measurement framework, however, this mostly leads to discarding the warmup values; therefore, we execute warmup + iterations and afterwards
    * filter the measured values
    * 
    * @return All iterations that should be carried out
    */
   @JsonIgnore
   public int getAllIterations() {
      return iterations + warmup;
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
      return kiekerConfig.isUseKieker();
   }

   public void setUseKieker(final boolean useKieker) {
      kiekerConfig.setUseKieker(useKieker);
   }

   public String getJavaVersion() {
      return javaVersion;
   }

   public void setJavaVersion(final String javaVersion) {
      this.javaVersion = javaVersion;
   }

   public MeasurementStrategy getMeasurementStrategy() {
      return measurementStrategy;
   }

   public void setMeasurementStrategy(final MeasurementStrategy measurementStrategy) {
      this.measurementStrategy = measurementStrategy;
   }

   public boolean isCallSyncBetweenVMs() {
      return callSyncBetweenVMs;
   }

   public void setCallSyncBetweenVMs(final boolean callSyncBetweenVMs) {
      this.callSyncBetweenVMs = callSyncBetweenVMs;
   }

   public int getWaitTimeBetweenVMs() {
      return waitTimeBetweenVMs;
   }

   public void setWaitTimeBetweenVMs(final int waitTimeBetweenVMs) {
      this.waitTimeBetweenVMs = waitTimeBetweenVMs;
   }

   /**
    * Returns the warmup that should be ignored when individual nodes are measured
    * 
    * @return
    */
   @JsonIgnore
   public int getNodeWarmup() {
      final int aggregationfactor = this.getKiekerConfig().isUseAggregation() ? this.getRepetitions() : 1;
      final int warmup = this.getWarmup() * this.getRepetitions() / aggregationfactor;
      return warmup;
   }

   public boolean isShowStart() {
      return executionConfig.isShowStart();
   }

   public void setShowStart(final boolean showStart) {
      executionConfig.setShowStart(showStart);
   }

   public ExecutionConfig getExecutionConfig() {
      return executionConfig;
   }

   public KiekerConfig getKiekerConfig() {
      return kiekerConfig;
   }
}