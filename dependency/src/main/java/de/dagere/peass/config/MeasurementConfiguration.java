package de.dagere.peass.config;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import de.dagere.peass.dependency.execution.ExecutionConfigMixin;
import de.dagere.peass.dependency.execution.MeasurementConfigurationMixin;
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
   private boolean useGC = true;
   private boolean executeBeforeClassInMeasurement = false;
   private boolean showStart = false;
   private boolean redirectToNull = true;

   @JsonIgnore
   private final KiekerConfiguration kiekerConfig;
   
   @JsonInclude(JsonInclude.Include.NON_DEFAULT)
   private boolean saveAll = true;
   
   private String javaVersion = System.getProperty("java.version");
   
   private MeasurementStrategy measurementStrategy = MeasurementStrategy.SEQUENTIAL;
   
   private StatisticsConfiguration statisticsConfig = new StatisticsConfiguration();
   private final ExecutionConfig executionConfig;

   public MeasurementConfiguration(final int vms) {
      executionConfig = new ExecutionConfig(20);
      kiekerConfig = new KiekerConfiguration();
      this.vms = vms;
   }

   public MeasurementConfiguration(final int vms, final ExecutionConfig executionConfig, final KiekerConfiguration kiekerConfig) {
      this.executionConfig = executionConfig;
      this.vms = vms;
      this.kiekerConfig = new KiekerConfiguration(kiekerConfig);
   }

   public MeasurementConfiguration(final int vms, final String version, final String versionOld) {
      executionConfig = new ExecutionConfig(20);
      kiekerConfig = new KiekerConfiguration();
      this.vms = vms;
      executionConfig.setVersion(version);
      executionConfig.setVersionOld(versionOld);
   }

   public MeasurementConfiguration(@JsonProperty("timeout") final int timeout,
         @JsonProperty("vms") final int vms,
         @JsonProperty("type1error") final double type1error,
         @JsonProperty("type2error") final double type2error) {
      executionConfig = new ExecutionConfig(timeout / (60 * 1000));
      kiekerConfig = new KiekerConfiguration();
      this.vms = vms;
      statisticsConfig.setType1error(type1error);
      statisticsConfig.setType2error(type2error);
   }

   public MeasurementConfiguration(final MeasurementConfigurationMixin mixin, final ExecutionConfigMixin executionMixin, final StatisticsConfigurationMixin statisticMixin) {
      executionConfig = new ExecutionConfig(executionMixin);
      kiekerConfig = new KiekerConfiguration();
      this.vms = mixin.getVms();
      statisticsConfig.setType1error(statisticMixin.getType1error());
      statisticsConfig.setType2error(statisticMixin.getType2error());
      statisticsConfig.setStatisticTest(statisticMixin.getStatisticTest());
      statisticsConfig.setOutlierFactor(statisticMixin.getOutlierFactor());
      setEarlyStop(mixin.isEarlyStop());
      setUseKieker(mixin.isUseKieker());
      setIterations(mixin.getIterations());
      setWarmup(mixin.getWarmup());
      setRepetitions(mixin.getRepetitions());
      setUseGC(mixin.isUseGC());
      setRecord(mixin.getRecord());
      setMeasurementStrategy(mixin.getMeasurementStrategy());
      
      

      saveAll = !mixin.isSaveNothing();
   }

   @JsonCreator
   public MeasurementConfiguration(@JsonProperty("timeout") final int timeout,
         @JsonProperty("vms") final int vms,
         @JsonProperty("earlystop") final boolean earlyStop,
         @JsonProperty("version") final String version,
         @JsonProperty("versionOld") final String versionOld) {
      executionConfig = new ExecutionConfig(timeout / (60 * 1000));
      kiekerConfig = new KiekerConfiguration();
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
      executionConfig = new ExecutionConfig(other.getExecutionConfig());
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
      
      this.redirectToNull = other.redirectToNull;
      kiekerConfig = new KiekerConfiguration();
      kiekerConfig.setUseKieker(other.isUseKieker());
      kiekerConfig.setUseSourceInstrumentation(other.isUseSourceInstrumentation());
      kiekerConfig.setUseSelectiveInstrumentation(other.isUseSelectiveInstrumentation());
      kiekerConfig.setUseSampling(other.isUseSampling());
      kiekerConfig.setUseCircularQueue(other.isUseCircularQueue());
      kiekerConfig.setEnableAdaptiveMonitoring(other.isEnableAdaptiveConfig());
      kiekerConfig.setAdaptiveInstrumentation(other.getKiekerConfig().isAdaptiveInstrumentation());
      kiekerConfig.setKiekerAggregationInterval(other.getKiekerAggregationInterval());
      kiekerConfig.setRecord(other.getRecord());
      this.useGC = other.useGC;
      this.javaVersion = other.javaVersion;
      this.measurementStrategy = other.measurementStrategy;
      this.saveAll = other.saveAll;
      this.executeBeforeClassInMeasurement = other.executeBeforeClassInMeasurement;
      this.showStart = other.showStart;
   }
   
   public StatisticsConfiguration getStatisticsConfig() {
      return statisticsConfig;
   }
   
   public void setStatisticsConfig(final StatisticsConfiguration statisticsConfig) {
      this.statisticsConfig = statisticsConfig;
   }

   public void setSaveAll(final boolean saveAll) {
      this.saveAll = saveAll;
   }
   
   public boolean isSaveAll() {
      return saveAll;
   }
   
   public boolean isRedirectSubprocessOutputToFile() {
      return executionConfig.isRedirectSubprocessOutputToFile();
   }

   public void setRedirectSubprocessOutputToFile(final boolean redirectSubprocesses) {
      executionConfig.setRedirectSubprocessOutputToFile(redirectSubprocesses);
   }

   public void setExecuteBeforeClassInMeasurement(final boolean executeBeforeClassInMeasurement) {
      this.executeBeforeClassInMeasurement = executeBeforeClassInMeasurement;
   }
   
   public boolean isExecuteBeforeClassInMeasurement() {
      return executeBeforeClassInMeasurement;
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
      return executionConfig.getTimeout();
   }

   @JsonIgnore
   public long getTimeoutInSeconds() {
      return executionConfig.getTimeoutInSeconds();
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
   
   /**
    * Warmup could be considered by the measurement framework, however, this mostly leads to discarding the warmup values; therefore, we execute warmup + iterations
    * and afterwards filter the measured values
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

   public int getKiekerAggregationInterval() {
      return kiekerConfig.getKiekerAggregationInterval();
   }

   public void setKiekerAggregationInterval(final int kiekerAggregationInterval) {
      kiekerConfig.setKiekerAggregationInterval(kiekerAggregationInterval);
   }

   public String getJavaVersion() {
      return javaVersion;
   }

   public void setJavaVersion(final String javaVersion) {
      this.javaVersion = javaVersion;
   }

   public void setRecord(final AllowedKiekerRecord record) {
      kiekerConfig.setRecord(record);
   }

   public AllowedKiekerRecord getRecord() {
      return kiekerConfig.getRecord();
   }

   public boolean isUseSourceInstrumentation() {
      return kiekerConfig.isUseSourceInstrumentation();
   }

   public void setUseSourceInstrumentation(final boolean useSourceInstrumentation) {
      kiekerConfig.setUseSourceInstrumentation(useSourceInstrumentation);
   }

   public boolean isUseCircularQueue() {
      return kiekerConfig.isUseCircularQueue();
   }

   public void setUseCircularQueue(final boolean useCircularQueue) {
      kiekerConfig.setUseCircularQueue(useCircularQueue);
   }

   public boolean isUseSelectiveInstrumentation() {
      return kiekerConfig.isUseSelectiveInstrumentation();
   }

   public void setUseSelectiveInstrumentation(final boolean useSelectiveInstrumentation) {
      kiekerConfig.setUseSelectiveInstrumentation(useSelectiveInstrumentation);
   }

   public boolean isUseSampling() {
      return kiekerConfig.isUseSampling();
   }

   public void setUseSampling(final boolean useSampling) {
      kiekerConfig.setUseSampling(useSampling);
   }

   public boolean isEnableAdaptiveConfig() {
      return kiekerConfig.isEnableAdaptiveMonitoring();
   }

   public void setEnableAdaptiveConfig(final boolean allowAdaptiveConfig) {
      kiekerConfig.setEnableAdaptiveMonitoring(allowAdaptiveConfig);
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

   public boolean isShowStart() {
      return showStart;
   }

   public void setShowStart(final boolean showStart) {
      this.showStart = showStart;
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

   @JsonIgnore
   public KiekerConfiguration getKiekerConfig() {
      return kiekerConfig;
   }
}