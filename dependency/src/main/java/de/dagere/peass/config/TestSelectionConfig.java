package de.dagere.peass.config;

import java.io.Serializable;

public class TestSelectionConfig implements Serializable {
   
   private static final long serialVersionUID = -3734493960077455640L;
   
   private final int threads;
   private final boolean doNotUpdateDependencies;
   private final boolean generateTraces;
   private final boolean generateCoverageSelection;
   private final boolean skipProcessSuccessRuns;

   public TestSelectionConfig(final int threads, final boolean doNotUpdateDependencies) {
      this.threads = threads;
      this.doNotUpdateDependencies = doNotUpdateDependencies;
      if (doNotUpdateDependencies) {
         generateTraces = false;
         generateCoverageSelection = false;
      } else {
         generateTraces = true;
      // Coverage selection does not create high additional effort after view generation, so generate it by default if views are generated
         generateCoverageSelection = true; 
      }
      skipProcessSuccessRuns = false;
   }

   public TestSelectionConfig(final int threads, final boolean doNotUpdateDependencies, final boolean generateViews, final boolean generateCoverageSelection) {
      this.threads = threads;
      this.doNotUpdateDependencies = doNotUpdateDependencies;
      this.generateTraces = generateViews;
      this.generateCoverageSelection = generateCoverageSelection;
      skipProcessSuccessRuns = false;
      check();
   }
   
   public TestSelectionConfig(final int threads, final boolean doNotUpdateDependencies, final boolean generateTraces, final boolean generateCoverageSelection, final boolean skipProcessSuccessRuns) {
      this.threads = threads;
      this.doNotUpdateDependencies = doNotUpdateDependencies;
      this.generateTraces = generateTraces;
      this.generateCoverageSelection = generateCoverageSelection;
      this.skipProcessSuccessRuns = skipProcessSuccessRuns;
      check();
   }

   private void check() {
      if (doNotUpdateDependencies && generateTraces) {
         throw new RuntimeException("isGenerateViews may only be true if doNotUpdateDependencies is false! "
               + "If doNotUpdateDependencies is set, no traces are generates; then it is not possible to generate views");
      }
      if (!generateTraces && generateCoverageSelection) {
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

   public boolean isGenerateTraces() {
      return generateTraces;
   }
   
   public boolean isGenerateCoverageSelection() {
      return generateCoverageSelection;
   }
   
   public boolean isSkipProcessSuccessRuns() {
      return skipProcessSuccessRuns;
   }
}
