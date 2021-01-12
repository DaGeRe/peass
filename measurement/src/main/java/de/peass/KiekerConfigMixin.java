package de.peass;

import picocli.CommandLine.Option;

public class KiekerConfigMixin {
   @Option(names = { "-writeInterval", "--writeInterval" }, description = "Interval for KoPeMe-aggregated-writing (in milliseconds)")
   public int writeInterval = 5000;

   @Option(names = { "-useSourceInstrumentation", "--useSourceInstrumentation" }, description = "Use source instrumentation (default false - AspectJ instrumentation is used)")
   public boolean useSourceInstrumentation = false;

   @Option(names = { "-useCircularQueue", "--useCircularQueue" }, description = "Use circular queue (default false - LinkedBlockingQueue is used)")
   public boolean useCircularQueue = false;

   @Option(names = { "-useSelectiveInstrumentation",
         "--useSelectiveInstrumentation" }, description = "Use selective instrumentation (only selected methods / classes are instrumented) - adaptive monitoring will not make sense")
   public boolean useSelectiveInstrumentation = false;

   @Option(names = { "-useSampling",
         "--useSampling" }, description = "Use sampling (only record every nth invocation of method - may reduce measurement noise)")
   public boolean useSampling = false;

   public int getWriteInterval() {
      return writeInterval;
   }

   public boolean isUseSourceInstrumentation() {
      return useSourceInstrumentation;
   }

   public boolean isUseCircularQueue() {
      return useCircularQueue;
   }

   public boolean isUseSelectiveInstrumentation() {
      return useSelectiveInstrumentation;
   }

   public boolean isUseSampling() {
      return useSampling;
   }
   
   
}
