package de.dagere.peass.config.parameters;

import java.util.LinkedHashSet;

import de.dagere.kopeme.kieker.writer.WritingType;
import de.dagere.peass.config.KiekerConfig;
import picocli.CommandLine.Option;

public class KiekerConfigMixin {

   @Option(names = { "-writeInterval", "--writeInterval" }, description = "Interval for KoPeMe-aggregated-writing (in milliseconds)")
   public int writeInterval = KiekerConfig.DEFAULT_WRITE_INTERVAL;

   @Option(names = { "-notUseSourceInstrumentation", "--notUseSourceInstrumentation" }, description = "Not use source instrumentation (disabling enables AspectJ instrumentation)")
   public boolean notUseSourceInstrumentation = false;

   @Option(names = { "-useCircularQueue", "--useCircularQueue" }, description = "Use circular queue (default false - LinkedBlockingQueue is used)")
   public boolean useCircularQueue = KiekerConfig.USE_CIRCULAR_QUEUE_DEFAULT;

   @Option(names = { "-notUseSelectiveInstrumentation",
         "--notUseSelectiveInstrumentation" }, description = "Use selective instrumentation (only selected methods / classes are instrumented) - is activated by default is source instrumentation is activated")
   public boolean notUseSelectiveInstrumentation = false;

   @Option(names = { "-notUseAggregation",
         "--notUseAggregation" }, description = "Not use aggregation (aggregate the measured values immediately in the system under test, "
               + "instead of summing them up after the execution). Is automatically disabled for regression test selection.")
   public boolean notUseAggregation = false;

   @Option(names = { "-measureAdded",
         "--measureAdded" }, description = "Measure call tree nodes that have been added or removed in one commit (might lead to less accuracy of measurements)")
   public boolean measureAdded = false;

   @Option(names = { "-useExtraction", "--useExtraction" }, description = "Extract methods when using source instrumentation")
   public boolean useExtraction = false;

   @Option(names = { "-enableAdaptiveInstrumentation", "--enableAdaptiveInstrumentation" }, description = "Enable adaptive instrumentation (for performance comparison to AspectJ)")
   public boolean enableAdaptiveInstrumentation = false;

   @Option(names = { "-traceSizeInMb", "--traceSizeInMb" }, description = "Sets the maximum allowed trace size in Mb (bigger traces will be ignored by Peass)")
   public long traceSizeInMb = KiekerConfig.DEFAULT_TRACE_SIZE_IN_MB;

   @Option(names = { "-kiekerQueueSize",
         "--kiekerQueueSize" }, description = "Sets the maximum queue size in Kieker (space is reserverd, consider increasing if queue entries are swallowed)")
   public long kiekerQueueSize = KiekerConfig.DEFAULT_KIEKER_QUEUE_SIZE;

   @Option(names = { "-onlyOneCallRecording",
         "--onlyOneCallRecording" }, description = "Only record calls once (ONLY allowed for regression test selection)")
   public boolean onlyOneCallRecording = false;

   @Option(names = { "-excludeForTracing", "--excludeForTracing" }, description = "Methods that are excluded for tracing in RTS and RCA (default: empty, excludes no method)")
   protected String[] excludeForTracing;

   @Option(names = { "-skipDefaultConstructor",
         "--skipDefaultConstructor" }, description = "Deactivates creation of the default constructor (required if Lombok is used)")
   protected boolean skipDefaultConstructor = false;

   @Option(names = { "-kiekerWaitTime", "--kiekerWaitTime" }, description = "Time that KoPeMe should wait until Kieker writing is finshed in seconds (default: 10)")
   protected int kiekerWaitTime = KiekerConfig.DEFAULT_KIEKER_WAIT_TIME;

   @Option(names = { "-writingType", "--writingType" }, description = "Writing type for RCA data. BinaryAggregated writes results in a given amount of seconds, BinarySimple writes results every individual repetition. The latter might produce big amounts of data, hence, BinaryAggregated is the default currently")
   protected WritingType writingType = WritingType.BinaryAggregated;
   
   @Option(names = { "-disableKiekerKoPeMe", "--disableKiekerKoPeMe" }, description = "Disables the Kieker management by KoPeMe ")
   protected boolean disableKiekerKoPeMe;
   
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

   public boolean isNotUseAggregation() {
      return notUseAggregation;
   }
   
   public boolean isMeasureAdded() {
      return measureAdded;
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

   public boolean isSkipDefaultConstructor() {
      return skipDefaultConstructor;
   }

   public void setSkipDefaultConstructor(final boolean skipDefaultConstructor) {
      this.skipDefaultConstructor = skipDefaultConstructor;
   }

   public int getKiekerWaitTime() {
      return kiekerWaitTime;
   }

   public void setKiekerWaitTime(final int kiekerWaitTime) {
      this.kiekerWaitTime = kiekerWaitTime;
   }
   
   public void setDisableKiekerKoPeMe(boolean disableKiekerKoPeMe) {
      this.disableKiekerKoPeMe = disableKiekerKoPeMe;
   }
   
   public boolean isDisableKiekerKoPeMe() {
      return disableKiekerKoPeMe;
   }

   public KiekerConfig getKiekerConfig() {
      KiekerConfig kiekerConfig = new KiekerConfig(true);
      kiekerConfig.setUseCircularQueue(useCircularQueue);
      kiekerConfig.setUseSelectiveInstrumentation(!notUseSelectiveInstrumentation);
      kiekerConfig.setUseAggregation(!notUseAggregation);
      kiekerConfig.setMeasureAdded(measureAdded);
      kiekerConfig.setExtractMethod(useExtraction);
      kiekerConfig.setAdaptiveInstrumentation(enableAdaptiveInstrumentation);
      kiekerConfig.setUseSourceInstrumentation(!notUseSourceInstrumentation);
      kiekerConfig.setKiekerQueueSize(kiekerQueueSize);
      kiekerConfig.setTraceSizeInMb(traceSizeInMb);
      kiekerConfig.setOnlyOneCallRecording(onlyOneCallRecording);
      kiekerConfig.setCreateDefaultConstructor(!skipDefaultConstructor);
      kiekerConfig.setWritingType(writingType);
      if (excludeForTracing != null) {
         LinkedHashSet<String> excludedForTracing = new LinkedHashSet<>();
         for (String exclude : excludeForTracing) {
            excludedForTracing.add(exclude);
         }
         kiekerConfig.setExcludeForTracing(excludedForTracing);
      }
      kiekerConfig.setDisableKiekerKoPeMe(disableKiekerKoPeMe);

      kiekerConfig.check();

      return kiekerConfig;
   }

}
