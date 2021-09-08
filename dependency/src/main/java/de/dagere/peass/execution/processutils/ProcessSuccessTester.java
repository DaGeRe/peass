package de.dagere.peass.execution.processutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.PeassFolders;
import de.dagere.peass.dependency.execution.EnvironmentVariables;

public class ProcessSuccessTester {
   
   private static final Logger LOG = LogManager.getLogger(ProcessSuccessTester.class);
   
   private final PeassFolders folders;
   private final MeasurementConfiguration measurementConfig;
   private final EnvironmentVariables env;
   
   public ProcessSuccessTester(final PeassFolders folders, final MeasurementConfiguration measurementConfig, final EnvironmentVariables env) {
      this.folders = folders;
      this.measurementConfig = measurementConfig;
      this.env = env;
   }

   public boolean testRunningSuccess(final String version, final String[] vars) {
      boolean isRunning = false;
      try {
         LOG.debug("Executing run success test {}", folders.getProjectFolder());
         final File versionFolder = getVersionFolder(version);
         final File logFile = new File(versionFolder, "testRunning.log");

         ProcessBuilderHelper builder = new ProcessBuilderHelper(env, folders);
         Process process = builder.buildFolderProcess(folders.getProjectFolder(), logFile, vars);
         LOG.debug("Waiting for {} minutes", measurementConfig.getTimeoutInSeconds());

         process.waitFor(measurementConfig.getTimeoutInSeconds(), TimeUnit.SECONDS);
         if (process.isAlive()) {
            LOG.debug("Destroying process");
            process.destroyForcibly().waitFor();
         }
         final int returncode = process.exitValue();
         if (returncode != 0) {
            LOG.info("Success test run failed");
            isRunning = false;
            printFailureLogToCommandline(logFile);
         } else {
            LOG.info("Test was successfull");
            isRunning = true;
         }
      } catch (final IOException e) {
         e.printStackTrace();
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }
      return isRunning;
   }
   
   public File getVersionFolder(final String version) {
      final File versionFolder = new File(folders.getDependencyLogFolder(), version);
      if (!versionFolder.exists()) {
         versionFolder.mkdir();
      }
      return versionFolder;
   }
   
   private void printFailureLogToCommandline(final File logFile) throws IOException, FileNotFoundException {
      try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
         String line;
         while ((line = br.readLine()) != null) {
            System.out.println(line);
         }
      }
   }
}
