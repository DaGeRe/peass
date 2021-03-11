package de.peass.config;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.peass.dependency.execution.ExecutionConfigMixin;
import de.peass.dependency.execution.MeasurementConfigurationMixin;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;

public class MeasurementConfiguration implements Serializable {
   
   private static final long serialVersionUID = -6936740902708676182L;

   public static final MeasurementConfiguration DEFAULT = new MeasurementConfiguration(60 * 1000, 30, 0.01, 0.01);

   private final int vms;
   private boolean earlyStop = true;
   private int warmup = 0;
   private int iterations = 1;
   private int repetitions = 1;
   private boolean logFullData = true;
   private boolean removeSnapshots = false;

   // Kieker config
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
   
   private StatisticsConfiguration statisticsConfig = new StatisticsConfiguration();
   private final ExecutionConfig executionConfig;

   public MeasurementConfiguration(final int vms) {
      executionConfig = new ExecutionConfig(20);
      this.vms = vms;
   }

   public MeasurementConfiguration(final int vms, final int timeoutInMinutes) {
      executionConfig = new ExecutionConfig(timeoutInMinutes);
      this.vms = vms;
   }

   public MeasurementConfiguration(final int vms, final String version, final String versionOld) {
      executionConfig = new ExecutionConfig(20);
      this.vms = vms;
      executionConfig.setVersion(version);
      executionConfig.setVersionOld(versionOld);
   }

   public MeasurementConfiguration(@JsonProperty("timeout") final int timeout,
         @JsonProperty("vms") final int vms,
         @JsonProperty("type1error") final double type1error,
         @JsonProperty("type2error") final double type2error) {
      executionConfig = new ExecutionConfig(timeout / (60 * 1000));
      this.vms = vms;
      statisticsConfig.setType1error(type1error);
      statisticsConfig.setType2error(type2error);
   }

   public MeasurementConfiguration(final MeasurementConfigurationMixin mixin, final ExecutionConfigMixin executionMixin) {
      this(executionMixin.getTimeout() * 60 * 1000, mixin.getVms(), mixin.getType1error(), mixin.getType2error());
      setEarlyStop(mixin.isEarlyStop());
      setUseKieker(mixin.isUseKieker());
      setIterations(mixin.getIterations());
      setWarmup(mixin.getWarmup());
      setRepetitions(mixin.getRepetitions());
      setUseGC(mixin.isUseGC());
      setRecord(mixin.getRecord());
      setMeasurementStrategy(mixin.getMeasurementStrategy());

      statisticsConfig.setOutlierFactor(mixin.getOutlierFactor());
      
      executionConfig.setVersion(executionMixin.getVersion());
      executionConfig.setVersionOld(executionMixin.getVersionOld());
      executionConfig.setStartversion(executionMixin.getStartversion());
      executionConfig.setEndversion(executionMixin.getEndversion());
      
      executionConfig.setTestGoal(executionMixin.getTestGoal());
      if (executionMixin.getIncludes() != null) {
         for (String include : executionMixin.getIncludes()) {
            executionConfig.getIncludes().add(include);
         }
      }
   }

   @JsonCreator
   public MeasurementConfiguration(@JsonProperty("timeout") final int timeout,
         @JsonProperty("vms") final int vms,
         @JsonProperty("earlystop") final boolean earlyStop,
         @JsonProperty("version") final String version,
         @JsonProperty("versionOld") final String versionOld) {
      executionConfig = new ExecutionConfig(timeout / (60 * 1000));
      this.vms = vms;
      this.earlyStop = earlyStop;
      executionConfig.setVersion(version);
      executionConfig.setVersionOld(versionOld);
   }

   /**
    * Copy constructor
    * 
    * @param other Configuration to copy
    */
   public MeasurementConfiguration(final MeasurementConfiguration other) {
      executionConfig = new ExecutionConfig(other.getTimeoutInMinutes());
      executionConfig.setTestGoal(other.getTestGoal());
      executionConfig.setIncludes(other.getIncludes());
      executionConfig.setVersion(other.getExecutionConfig().getVersion());
      executionConfig.setVersionOld(other.getExecutionConfig().getVersionOld());
      executionConfig.setStartversion(other.getExecutionConfig().getStartversion());
      executionConfig.setEndversion(other.getExecutionConfig().getEndversion());
      this.vms = other.vms;
      statisticsConfig.setType1error(other.getType1error());
      statisticsConfig.setType2error(other.getType2error());
      statisticsConfig.setOutlierFactor(other.getStatisticsConfig().getOutlierFactor());
      statisticsConfig.setStatisticTest(other.getStatisticsConfig().getStatisticTest());
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
      
   }
   
   public StatisticsConfiguration getStatisticsConfig() {
      return statisticsConfig;
   }
   
   public void setStatisticsConfig(final StatisticsConfiguration statisticsConfig) {
      this.statisticsConfig = statisticsConfig;
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

   public int getTimeout() {
      return executionConfig.getTimeout();
   }

   @JsonIgnore
   public int getTimeoutInMinutes() {
      return executionConfig.getTimeoutInMinutes();
   }

   public int getVms() {
      return vms;
   }

   @JsonProperty(access = JsonProperty.Access.READ_ONLY)
   public double getType1error() {
      return statisticsConfig.getType1error();
   }

   public void setType1error(final double type1error) {
      statisticsConfig.setType1error(type1error);
   }

   @JsonProperty(access = JsonProperty.Access.READ_ONLY)
   public double getType2error() {
      return statisticsConfig.getType2error();
   }

   public void setType2error(final double type2error) {
      statisticsConfig.setType2error(type2error);
   }

   public boolean isEarlyStop() {
      return earlyStop;
   }

   public void setEarlyStop(final boolean earlyStop) {
      this.earlyStop = earlyStop;
   }

   public String getVersion() {
      return executionConfig.getVersion();
   }

   public void setVersion(final String version) {
      executionConfig.setVersion(version);
   }

   public String getVersionOld() {
      return executionConfig.getVersionOld();
   }

   public void setVersionOld(final String versionOld) {
      executionConfig.setVersionOld(versionOld);
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
    * 
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

   @JsonProperty(access = JsonProperty.Access.READ_ONLY)
   public String getTestGoal() {
      return executionConfig.getTestGoal();
   }
   
   public void setTestGoal(final String testGoal) {
      executionConfig.setTestGoal(testGoal);
   }

   @JsonProperty(access = JsonProperty.Access.READ_ONLY)
   public List<String> getIncludes() {
      return executionConfig.getIncludes();
   }

   public void setIncludes(final List<String> includes) {
      executionConfig.setIncludes(includes);
   }

   public ExecutionConfig getExecutionConfig() {
      return executionConfig;
   }
}