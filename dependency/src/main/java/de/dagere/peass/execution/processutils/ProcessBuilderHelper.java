package de.dagere.peass.execution.processutils;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.datastorage.ParamNameHelper;
import de.dagere.kopeme.junit.rule.annotations.KoPeMeConstants;
import de.dagere.peass.execution.utils.CommandConcatenator;
import de.dagere.peass.execution.utils.EnvironmentVariables;
import de.dagere.peass.folders.PeassFolders;
import de.dagere.peass.utils.StreamGobbler;

public class ProcessBuilderHelper {

   private static final Logger LOG = LogManager.getLogger(ProcessBuilderHelper.class);

   private final EnvironmentVariables env;
   private final PeassFolders folders;
   private int chosenIndex = -1;

   public ProcessBuilderHelper(final EnvironmentVariables env, final PeassFolders folders) {
      this.env = env;
      this.folders = folders;
   }

   public Process buildFolderProcess(final File currentFolder, final File logFile, final String[] vars) throws IOException {
      String[] envPropertyArray = env.getProperties().length() > 0 ? env.getProperties().split(" ") : new String[0];
      final String[] varsWithProperties = CommandConcatenator.concatenateCommandArrays(vars, envPropertyArray);
      LOG.debug("Command: {}", Arrays.toString(varsWithProperties));

      final ProcessBuilder pb = new ProcessBuilder(varsWithProperties);
      overwriteEnvVars(pb);

      if (chosenIndex != -1) {
         pb.environment().put(KoPeMeConstants.KOPEME_CHOSEN_PARAMETER_INDEX, Integer.toString(chosenIndex));
      }

      pb.directory(currentFolder);
      if (logFile != null) {
         pb.redirectOutput(Redirect.appendTo(logFile));
         pb.redirectError(Redirect.appendTo(logFile));
      }

      final Process process = pb.start();
      printPIDInfo(logFile);
      return process;
   }

   private void overwriteEnvVars(final ProcessBuilder pb) {
      LOG.debug("KOPEME_HOME={}", folders.getTempMeasurementFolder().getAbsolutePath());
      pb.environment().put("KOPEME_HOME", folders.getTempMeasurementFolder().getAbsolutePath());
      String ldLibraryPath = System.getenv().get("LD_LIBRARY_PATH");
      if (ldLibraryPath != null) {
         LOG.debug("LD_LIBRARY_PATH: {}", ldLibraryPath);
      }
      for (final Map.Entry<String, String> env : System.getenv().entrySet()) {
         pb.environment().put(env.getKey(), env.getValue());
      }

      for (Map.Entry<String, String> entry : env.getEnvironmentVariables().entrySet()) {
         LOG.trace("Environment: {} = {}", entry.getKey(), entry.getValue());
         pb.environment().put(entry.getKey(), entry.getValue());
      }
   }

   private void printPIDInfo(final File logFile) throws IOException {
      if (EnvironmentVariables.isLinux()) {
         String usedPidCountString = new File("/proc/self").getCanonicalFile().getName();
         if (usedPidCountString.matches("[0-9]+")) {
            final int pid = Integer.parseInt(usedPidCountString);
            LOG.debug("Process started: {} Used PIDs: {} Log to: {}", pid, getProcessCount(), logFile);
         } else {
            LOG.debug("PID count could not be parsed: {} Operating System: {}", usedPidCountString, System.getProperty("os.name"));
         }
      }
   }

   public synchronized static int getProcessCount() {
      int count = -1;
      try {
         final Process process = new ProcessBuilder(new String[] { "bash", "-c", "ps -e -T | wc -l" }).start();
         final String result = StreamGobbler.getFullProcess(process, false).replaceAll("\n", "").replace("\r", "");
         count = Integer.parseInt(result.trim());
      } catch (NumberFormatException e) {
         // This is expected on systems without ps and will not be logged
         if (!e.getMessage().equals("For input string: \"bash: line 1: ps: command not found0\"")
               && !e.getMessage().equals("For input string: \"0bash: line 1: ps: command not found\"")) {
            e.printStackTrace();
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
      return count;
   }

   /**
    * If on linux, this calls the sync command, which should assure that pending hard disc writes are finished and the next test execution is done with clear write buffers
    * (especially of Kieker)
    */
   public static void syncToHdd() {
      if (EnvironmentVariables.isLinux()) {
         LOG.info("Syncing to hdd");
         try {
            Process syncProcess = Runtime.getRuntime().exec("sync");
            StreamGobbler.showFullProcess(syncProcess);
         } catch (IOException e) {
            e.printStackTrace();
         }
      } else {
         LOG.info("No call to sync, since not on Linux");
      }
   }

   public void parseParams(String params) {
      if (params != null) {
         if (params.matches(KoPeMeConstants.JUNIT_PARAMETERIZED + ParamNameHelper.PARAM_VALUE_SEPARATOR + "[0-9]+")) {
            String indexString = params.substring(params.indexOf(ParamNameHelper.PARAM_VALUE_SEPARATOR) + 1);
            chosenIndex = Integer.parseInt(indexString);
         }
      }
   }
}
