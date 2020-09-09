package de.peass;

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

}
