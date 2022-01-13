package de.dagere.peass.config.parameters;

import de.dagere.peass.config.KiekerConfig;
import picocli.CommandLine.Option;

public class KiekerConfigMixin {
   
   
   @Option(names = { "-writeInterval", "--writeInterval" }, description = "Interval for KoPeMe-aggregated-writing (in milliseconds)")
   public int writeInterval =KiekerConfig.DEFAULT_WRITE_INTERVAL;

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
  
   @Option(names = { "-traceSizeInMb", "--traceSizeInMb" }, description = "Sets the maximum allowed trace size in Mb (bigger traces will be ignored by Peass)")
   public long traceSizeInMb = 100;
   
   @Option(names = { "-kiekerQueueSize", "--kiekerQueueSize" }, description = "Sets the maximum queue size in Kieker (space is reserverd, consider increasing if queue entries are swallowed)")
   public long kiekerQueueSize = KiekerConfig.DEFAULT_KIEKER_QUEUE_SIZE;

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
   
   public long getTraceSizeInMb() {
      return traceSizeInMb;
   }
   
   public void setTraceSizeInMb(final long traceSizeInMb) {
      this.traceSizeInMb = traceSizeInMb;
   }

   public KiekerConfig getKiekerConfig() {
      KiekerConfig kiekerConfig = new KiekerConfig(true);
      kiekerConfig.setUseCircularQueue(useCircularQueue);
      kiekerConfig.setUseSelectiveInstrumentation(!notUseSelectiveInstrumentation);
      kiekerConfig.setUseAggregation(useAggregation);
      kiekerConfig.setExtractMethod(useExtraction);
      kiekerConfig.setAdaptiveInstrumentation(enableAdaptiveInstrumentation);
      kiekerConfig.setUseSourceInstrumentation(!notUseSourceInstrumentation);
      kiekerConfig.setKiekerQueueSize(kiekerQueueSize);
      kiekerConfig.setTraceSizeInMb(traceSizeInMb);
      
      kiekerConfig.check();
      
      return kiekerConfig;
   }

}
