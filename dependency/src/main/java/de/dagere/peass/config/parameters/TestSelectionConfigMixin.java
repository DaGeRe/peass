package de.dagere.peass.config.parameters;

import java.io.File;

import de.dagere.peass.config.TestSelectionConfig;
import picocli.CommandLine.Option;

public class TestSelectionConfigMixin {
   @Option(names = { "-folder", "--folder" }, description = "Folder that should be analyzed", required = true)
   private File projectFolder;

   @Option(names = { "-out", "--out" }, description = "Folder for results")
   private File resultBaseFolder = new File("results");

   @Option(names = { "-threads", "--threads" }, description = "Number of parallel threads for analysis, default 2")
   private int threads = 2;

   @Option(names = {"-doNotUpdateDependencies", "--doNotUpdateDependencies"}, description = "Disable updating of dependencies. This will make results for more than one version unusable, but increase dependency creation speed.")
   public boolean doNotUpdateDependencies = false;
   
   @Option(names = {"-doNotGenerateTraces", "--doNotGenerateTraces"}, description = "Disable generation of traces (and thereby trace-diffs). Is false by default, but will be activated automatically if --doNotUpdateDependencies is set.")
   public boolean doNotGenerateTraces = false;
   
   @Option(names = {"-skipProcessSuccessRuns", "--skipProcessSuccessRuns"}, description = "Skips the process success run. ")
   public boolean skipProcessSuccessRuns = false;
   
   @Option(names = {"-doNotGenerateCoverageSelection", "--doNotGenerateCoverageSelection"}, description = "Disables coverage selection. Is false by default, but will be activated automatically if --doNotGenerateCoverageSelection is set.")
   public boolean doNotGenerateCoverageSelection = false;
   
   @Option(names = {"-doNotGenerateProperties", "--doNotGenerateProperties"}, description = "Disables properties generation. By default, properties will be generated.")
   public boolean doNotGenerateProperties = false;
   
   public File getProjectFolder() {
      return projectFolder;
   }

   public File getResultBaseFolder() {
      return resultBaseFolder;
   }

   public int getThreads() {
      return threads;
   }

   public void setDoNotUpdateDependencies(final boolean doNotUpdateDependencies) {
      this.doNotUpdateDependencies = doNotUpdateDependencies;
   }
   
   public boolean isDoNotUpdateDependencies() {
      return doNotUpdateDependencies;
   }
   
   public boolean isDoNotGenerateProperties() {
      return doNotGenerateProperties;
   }
   
   public TestSelectionConfig getDependencyConfig() {
      if (doNotUpdateDependencies) {
         doNotGenerateCoverageSelection = true;
         doNotGenerateTraces = true;
      }
      boolean generateTraces = !doNotGenerateTraces;
      boolean generateCoverageSelection = !doNotGenerateCoverageSelection;
      return new TestSelectionConfig(threads, doNotUpdateDependencies, generateTraces, generateCoverageSelection, skipProcessSuccessRuns);
   }
}
