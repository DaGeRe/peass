package de.dagere.peass.config;

import java.io.Serializable;

public class DependencyConfig implements Serializable {
   
   private static final long serialVersionUID = -3734493960077455640L;
   
   private final int threads;
   private final boolean doNotUpdateDependencies;
   private final boolean generateViews;
   private final boolean generateCoverageSelection;
   private final boolean skipProcessSuccessRuns;

   public DependencyConfig(final int threads, final boolean doNotUpdateDependencies) {
      this.threads = threads;
      this.doNotUpdateDependencies = doNotUpdateDependencies;
      if (doNotUpdateDependencies) {
         generateViews = false;
         generateCoverageSelection = false;
      } else {
         generateViews = true;
      // Coverage selection does not create high additional effort after view generation, so generate it by default if views are generated
         generateCoverageSelection = true; 
      }
      skipProcessSuccessRuns = false;
   }

   public DependencyConfig(final int threads, final boolean doNotUpdateDependencies, final boolean generateViews, final boolean generateCoverageSelection) {
      this.threads = threads;
      this.doNotUpdateDependencies = doNotUpdateDependencies;
      this.generateViews = generateViews;
      this.generateCoverageSelection = generateCoverageSelection;
      skipProcessSuccessRuns = false;
      check();
   }
   
   public DependencyConfig(final int threads, final boolean doNotUpdateDependencies, final boolean generateViews, final boolean generateCoverageSelection, final boolean skipProcessSuccessRuns) {
      this.threads = threads;
      this.doNotUpdateDependencies = doNotUpdateDependencies;
      this.generateViews = generateViews;
      this.generateCoverageSelection = generateCoverageSelection;
      this.skipProcessSuccessRuns = skipProcessSuccessRuns;
      check();
   }

   private void check() {
      if (doNotUpdateDependencies && generateViews) {
         throw new RuntimeException("isGenerateViews may only be true if doNotUpdateDependencies is false! "
               + "If doNotUpdateDependencies is set, no traces are generates; then it is not possible to generate views");
      }
      if (!generateViews && generateCoverageSelection) {
         throw new RuntimeException("generateCoverageSelection may only be true if generateViews is true! "
               + "If generateViews is disabled, no traces are generates; then it is not possible to select by code coverage");
      }
   }

   public int getThreads() {
      return threads;
   }

   public boolean isDoNotUpdateDependencies() {
      return doNotUpdateDependencies;
   }

   public boolean isGenerateViews() {
      return generateViews;
   }
   
   public boolean isGenerateCoverageSelection() {
      return generateCoverageSelection;
   }
   
   public boolean isSkipProcessSuccessRuns() {
      return skipProcessSuccessRuns;
   }
}
