package de.dagere.peass.config;

public class DependencyConfig {
   private final int threads;
   private final boolean doNotUpdateDependencies;
   private final boolean isGenerateViews;

   public DependencyConfig(final int threads, final boolean doNotUpdateDependencies) {
      this.threads = threads;
      this.doNotUpdateDependencies = doNotUpdateDependencies;
      if (doNotUpdateDependencies) {
         isGenerateViews = false;
      } else {
         isGenerateViews = true;
      }
   }

   public DependencyConfig(final int threads, final boolean doNotUpdateDependencies, final boolean isGenerateViews) {
      this.threads = threads;
      this.doNotUpdateDependencies = doNotUpdateDependencies;
      this.isGenerateViews = isGenerateViews;
      if (doNotUpdateDependencies && isGenerateViews) {
         throw new RuntimeException("isGenerateViews may only be true if doNotUpdateDependencies is false! "
               + "If doNotUpdateDependencies is set, no traces are generates; then it is not possible to generate views");
      }
   }

   public int getThreads() {
      return threads;
   }

   public boolean isDoNotUpdateDependencies() {
      return doNotUpdateDependencies;
   }

   public boolean isGenerateViews() {
      return isGenerateViews;
   }

}
