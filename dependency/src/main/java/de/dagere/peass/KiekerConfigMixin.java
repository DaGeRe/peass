package de.dagere.peass;

import de.dagere.peass.config.KiekerConfiguration;
import picocli.CommandLine.Option;

public class KiekerConfigMixin {
   @Option(names = { "-writeInterval", "--writeInterval" }, description = "Interval for KoPeMe-aggregated-writing (in milliseconds)")
   public int writeInterval = 5000;

   @Option(names = { "-notUseSourceInstrumentation", "--notUseSourceInstrumentation" }, description = "Not use source instrumentation (disabling enables AspectJ instrumentation)")
   public boolean notUseSourceInstrumentation = false;

   @Option(names = { "-useCircularQueue", "--useCircularQueue" }, description = "Use circular queue (default false - LinkedBlockingQueue is used)")
   public boolean useCircularQueue = false;

   @Option(names = { "-notUseSelectiveInstrumentation",
         "--notUseSelectiveInstrumentation" }, description = "Use selective instrumentation (only selected methods / classes are instrumented) - is activated by default is source instrumentation is activated")
   public boolean notUseSelectiveInstrumentation = false;

   @Option(names = { "-useAggregation",
         "--useAggregation" }, description = "Use aggregation (only record every nth invocation of method - may reduce measurement noise)")
   public boolean useAggregation = false;

   @Option(names = { "-useExtraction", "--useExtraction" }, description = "Extract methods when using source instrumentation")
   public boolean useExtraction = false;
   
   @Option(names = { "-enableAdaptiveInstrumentation", "--enableAdaptiveInstrumentation" }, description = "Enable adaptive instrumentation (for performance comparison to AspectJ)")
   public boolean enableAdaptiveInstrumentation = false;

   public int getWriteInterval() {
      return writeInterval;
   }

   public boolean isNotUseSourceInstrumentation() {
      return notUseSourceInstrumentation;
   }

   public boolean isUseCircularQueue() {
      return useCircularQueue;
   }

   public boolean isNotUseSelectiveInstrumentation() {
      return notUseSelectiveInstrumentation;
   }

   public boolean isUseAggregation() {
      return useAggregation;
   }
   
   public boolean isUseExtraction() {
      return useExtraction;
   }
   
   public boolean isEnableAdaptiveInstrumentation() {
      return enableAdaptiveInstrumentation;
   }

   public KiekerConfiguration getKiekerConfig() {
      KiekerConfiguration kiekerConfig = new KiekerConfiguration(true);
      kiekerConfig.setUseCircularQueue(useCircularQueue);
      kiekerConfig.setUseSelectiveInstrumentation(!notUseSelectiveInstrumentation);
      kiekerConfig.setUseAggregation(useAggregation);
      kiekerConfig.setExtractMethod(useExtraction);
      kiekerConfig.setAdaptiveInstrumentation(enableAdaptiveInstrumentation);
      kiekerConfig.setUseSourceInstrumentation(!notUseSourceInstrumentation);
      return kiekerConfig;
   }

}
