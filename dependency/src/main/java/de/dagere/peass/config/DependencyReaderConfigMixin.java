package de.dagere.peass.config;

import java.io.File;

import picocli.CommandLine.Option;

public class DependencyReaderConfigMixin {
   @Option(names = { "-folder", "--folder" }, description = "Folder that should be analyzed", required = true)
   private File projectFolder;

   @Option(names = { "-out", "--out" }, description = "Folder for results")
   private File resultBaseFolder = new File("results");

   @Option(names = { "-threads", "--threads" }, description = "Number of parallel threads for analysis, default 2")
   private int threads = 2;

   @Option(names = {"-doNotUpdateDependencies", "--doNotUpdateDependencies"}, description = "Disable updating of dependencies. This will make results for more than one version unusable, but increase dependency creation speed.")
   public boolean doNotUpdateDependencies = false;
   
   @Option(names = {"-doNotGenerateViews", "--doNotGenerateViews"}, description = "Disable generation of views. Is false by default, but will be activated automatically if --doNotUpdateDependencies is set.")
   public boolean doNotGenerateViews = false;
   
   @Option(names = {"-skipProcessSuccessRuns", "--skipProcessSuccessRuns"}, description = "Skips the process success run. ")
   public boolean skipProcessSuccessRuns = false;
   
   @Option(names = {"-doNotGenerateCoverageSelection", "--doNotGenerateCoverageSelection"}, description = "Disables coverage selection. Is false by default, but will be activated automatically if --doNotGenerateCoverageSelection is set.")
   public boolean doNotGenerateCoverageSelection = false;
   
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
   
   public DependencyConfig getDependencyConfig() {
      if (doNotUpdateDependencies) {
         doNotGenerateCoverageSelection = true;
         doNotGenerateViews = true;
      }
      return new DependencyConfig(threads, doNotUpdateDependencies, !doNotGenerateViews, !doNotGenerateCoverageSelection, skipProcessSuccessRuns);
   }
}
