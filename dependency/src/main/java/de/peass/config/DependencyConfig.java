package de.peass.config;

public class DependencyConfig {
   private final int threads;
   private final boolean doNotUpdateDependencies;

   public DependencyConfig(final int threads, final boolean doNotUpdateDependencies) {
      this.threads = threads;
      this.doNotUpdateDependencies = doNotUpdateDependencies;
   }

   public int getThreads() {
      return threads;
   }

   public boolean isDoNotUpdateDependencies() {
      return doNotUpdateDependencies;
   }

}
