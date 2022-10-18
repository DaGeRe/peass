package de.dagere.peass.config;

import java.io.Serializable;

public class TestSelectionConfig implements Serializable {
   
   private static final long serialVersionUID = -3734493960077455640L;
   
   private final int threads;
   private final boolean doNotUpdateDependencies;
   private final boolean generateTraces;
   private final boolean generateCoverageSelection;
   private final boolean generateTwiceExecutability;
   private final boolean skipProcessSuccessRuns;
   private final boolean writeAsZip;

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
      generateTwiceExecutability = false;
      skipProcessSuccessRuns = false;
      writeAsZip = false;
   }

   public TestSelectionConfig(final int threads, final boolean doNotUpdateDependencies, final boolean generateViews, final boolean generateCoverageSelection, boolean generateTwiceExecutability, boolean writeAsZip) {
      this.threads = threads;
      this.doNotUpdateDependencies = doNotUpdateDependencies;
      this.generateTraces = generateViews;
      this.generateCoverageSelection = generateCoverageSelection;
      this.generateTwiceExecutability = generateTwiceExecutability;
      this.writeAsZip = writeAsZip;
      skipProcessSuccessRuns = false;
      check();
   }
   
   public TestSelectionConfig(final int threads, final boolean doNotUpdateDependencies, final boolean generateTraces, final boolean generateCoverageSelection, boolean generateTwiceExecutability, final boolean skipProcessSuccessRuns, boolean writeAsZip) {
      this.threads = threads;
      this.doNotUpdateDependencies = doNotUpdateDependencies;
      this.generateTraces = generateTraces;
      this.generateCoverageSelection = generateCoverageSelection;
      this.generateTwiceExecutability = generateTwiceExecutability;
      this.skipProcessSuccessRuns = skipProcessSuccessRuns;
      this.writeAsZip = writeAsZip;
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
      if (!generateTraces && generateTwiceExecutability) {
         throw new RuntimeException("If generateTwiceExecutability is true, generateTraces should be true.");
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
   
   public boolean isGenerateTwiceExecutability() {
      return generateTwiceExecutability;
   }
   
   public boolean isSkipProcessSuccessRuns() {
      return skipProcessSuccessRuns;
   }
   
   public boolean isWriteAsZip() {
      return writeAsZip;
   }
}
