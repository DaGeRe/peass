package de.peass.config;

import java.io.File;

import picocli.CommandLine.Option;

public class DependencyReaderConfig {
   @Option(names = { "-folder", "--folder" }, description = "Folder that should be analyzed", required = true)
   private File projectFolder;

   @Option(names = { "-out", "--out" }, description = "Folder for results")
   private File resultBaseFolder = new File("results");

   @Option(names = { "-timeout", "--timeout" }, description = "Timeout for each VM start")
   private int timeout = 5;

   @Option(names = { "-threads", "--threads" }, description = "Number of parallel threads for analysis")
   private int threads = 4;

   @Option(names = { "-startversion", "--startversion" }, description = "First version that should be analysed")
   private String startversion;

   @Option(names = { "-endversion", "--endversion" }, description = "Last version that should be analysed")
   private String endversion;
   
   @Option(names = { "-testGoal", "--testGoal" }, description = "Test goal that should be used; default testRelease for Android projects and test for all others. "
         + "If you want to use test<VariantName> for Android, please specify a goal (i.e. task name) here."
         + "If you want to run integration tests in maven e.g. by calling failsafe, also specify it here. ")
   private String testGoal;
   
   @Option(names = { "-includes", "--includes" }, description = "Testcases for inclusion (default: empty, includes all tests)")
   protected String[] includes;
   
   @Option(names = { "-pl", "--pl" }, description = "Projectlist (-pl) argument for maven (e.g. :submodule) - only the submodule and its dependencies are analyzed (using -am)")
   protected String pl;
   
   public String getTestGoal() {
      return testGoal;
   }

   public File getProjectFolder() {
      return projectFolder;
   }

   public File getResultBaseFolder() {
      return resultBaseFolder;
   }

   public int getTimeout() {
      return timeout;
   }

   public int getThreads() {
      return threads;
   }

   public String getStartversion() {
      return startversion;
   }

   public String getEndversion() {
      return endversion;
   }

   public ExecutionConfig getExecutionConfig() {
      ExecutionConfig executionConfig = new ExecutionConfig();
      executionConfig.setTestGoal(testGoal);
      if (includes != null) {
         for (String include : includes) {
            executionConfig.getIncludes().add(include);
         }
      }
      executionConfig.setPl(pl);
      return executionConfig;
   }
   
   public void setIncludes(final String[] includes) {
      this.includes = includes;
   }
   
   public String[] getIncludes() {
      return includes;
   }
   
   public String getPl() {
      return pl;
   }
   
   public void setPl(final String pl) {
      this.pl = pl;
   }

}
