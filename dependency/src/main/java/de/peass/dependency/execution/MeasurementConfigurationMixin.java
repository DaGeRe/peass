package de.peass.dependency.execution;

import picocli.CommandLine.Option;

public class MeasurementConfigurationMixin {
   @Option(names = { "-vms", "--vms" }, description = "Number of VMs to start")
   int vms = 100;

   @Option(names = { "-duration", "--duration" }, description = "Which duration to use - if duration is specified, warmup and iterations are ignored")
   int duration = 0;

   @Option(names = { "-warmup", "--warmup" }, description = "Number of warmup iterations")
   int warmup = 10;

   @Option(names = { "-iterations", "--iterations" }, description = "Number of iterations")
   int iterations = 1000;

   @Option(names = { "-repetitions", "--repetitions" }, description = "Last version that should be analysed")
   int repetitions = 100;

   @Option(names = { "-useKieker", "--useKieker", "-usekieker", "--usekieker" }, description = "Whether Kieker should be used")
   boolean useKieker = false;

   @Option(names = { "-useGC", "--useGC" }, description = "Do execute GC before each iteration (default false)")
   public boolean useGC = false;
   
   @Option(names = { "-earlyStop", "--earlyStop" }, description = "Whether to stop early (i.e. execute VMs until type 1 and type 2 error are met)")
   protected boolean earlyStop = false;

   @Option(names = { "-type1error",
         "--type1error" }, description = "Type 1 error of agnostic-t-test, i.e. probability of considering measurements equal when they are unequal (requires earlyStop)")
   public double type1error = 0.05;

   @Option(names = { "-type2error",
         "--type2error" }, description = "Type 2 error of agnostic-t-test, i.e. probability of considering measurements unequal when they are equal (requires earlyStop)")
   protected double type2error = 0.01;

   public int getVms() {
      return vms;
   }

   public int getDuration() {
      return duration;
   }

   public int getWarmup() {
      return warmup;
   }

   public int getIterations() {
      return iterations;
   }

   public int getRepetitions() {
      return repetitions;
   }

   public boolean isUseKieker() {
      return useKieker;
   }

   public boolean isUseGC() {
      return useGC;
   }

   public boolean isEarlyStop() {
      return earlyStop;
   }

   public double getType1error() {
      return type1error;
   }

   public double getType2error() {
      return type2error;
   }
   
   
}
