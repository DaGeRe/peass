package de.dagere.peass.execution.gradle;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.utils.StreamGobbler;

public class AnboxDirHandler {
   
   private static final Logger LOG = LogManager.getLogger(AnboxDirHandler.class);
   
   private final String adbCall;
   
   public AnboxDirHandler(String adb) {
      this.adbCall = adb;
   }

   public void removeDirInEmulator(String path) {
      String shellCommand = String.format("rm -fr %s", path);
      ProcessBuilder builder = new ProcessBuilder(adbCall, "shell", shellCommand);
      LOG.debug("ADB: Removing directory {}", path);

      try {
         Process process = builder.start();
         StreamGobbler.showFullProcess(process);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
   
   public void createDirInEmulator(String path) {
      String shellCommand = String.format("mkdir -p %s", path);
      ProcessBuilder builder = new ProcessBuilder(adbCall, "shell", shellCommand);
      LOG.debug("ADB: Creating directory {}", path);

      try {
         Process process = builder.start();
         StreamGobbler.showFullProcess(process);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
}
