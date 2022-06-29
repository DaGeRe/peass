package de.dagere.peass.execution.processutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;

public class ProcessSuccessTester {
   
   private static final Logger LOG = LogManager.getLogger(ProcessSuccessTester.class);
   
   private final PeassFolders folders;
   private final MeasurementConfig measurementConfig;
   private final EnvironmentVariables env;
   
   public ProcessSuccessTester(final PeassFolders folders, final MeasurementConfig measurementConfig, final EnvironmentVariables env) {
      this.folders = folders;
      this.measurementConfig = measurementConfig;
      this.env = env;
   }

   public boolean testRunningSuccess(final String commit, final String[] vars) {
      boolean isRunning = false;
      try {
         isRunning = testRunning(commit, vars);
      } catch (final IOException e) {
         e.printStackTrace();
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }
      return isRunning;
   }

   private boolean testRunning(final String commit, final String[] vars) throws IOException, InterruptedException, FileNotFoundException {
      boolean isRunning;
      LOG.debug("Executing run success test {}", folders.getProjectFolder());
      final File logFile = folders.getDependencyLogSuccessRunFile(commit);

      Process process = startProcess(vars, logFile);
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
      return isRunning;
   }

   private Process startProcess(final String[] vars, final File logFile) throws IOException, InterruptedException {
      ProcessBuilderHelper builder = new ProcessBuilderHelper(env, folders);
      Process process = builder.buildFolderProcess(folders.getProjectFolder(), logFile, vars);
      LOG.debug("Waiting for {} minutes", measurementConfig.getTimeoutInSeconds());

      process.waitFor(measurementConfig.getTimeoutInSeconds(), TimeUnit.SECONDS);
      return process;
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
