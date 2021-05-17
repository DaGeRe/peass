package de.dagere.peass.execution.processutils;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.PeASSFolders;
import de.dagere.peass.dependency.execution.CommandConcatenator;
import de.dagere.peass.dependency.execution.EnvironmentVariables;
import de.dagere.peass.utils.StreamGobbler;

public class ProcessBuilderHelper {
   
   private static final Logger LOG = LogManager.getLogger(ProcessBuilderHelper.class);
   
   private final EnvironmentVariables env;
   private final PeASSFolders folders;
   
   public ProcessBuilderHelper(final EnvironmentVariables env, final PeASSFolders folders) {
      this.env = env;
      this.folders = folders;
   }

   public Process buildFolderProcess(final File currentFolder, final File logFile, final String[] vars) throws IOException {
      String[] envPropertyArray = env.getProperties().length() > 0 ? env.getProperties().split(" ") : new String[0];
      final String[] varsWithProperties = CommandConcatenator.concatenateCommandArrays(vars, envPropertyArray);
      LOG.debug("Command: {}", Arrays.toString(varsWithProperties));

      final ProcessBuilder pb = new ProcessBuilder(varsWithProperties);
      overwriteEnvVars(pb);

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
//      if (this instanceof GradleTestExecutor) {
         pb.environment().put("GRADLE_HOME", folders.getGradleHome().getAbsolutePath());
//      }
      LOG.debug("LD_LIBRARY_PATH: {}", System.getenv().get("LD_LIBRARY_PATH"));
      for (final Map.Entry<String, String> env : System.getenv().entrySet()) {
         pb.environment().put(env.getKey(), env.getValue());
      }

      for (Map.Entry<String, String> entry : env.getEnvironmentVariables().entrySet()) {
         LOG.trace("Environment: {} = {}", entry.getKey(), entry.getValue());
         pb.environment().put(entry.getKey(), entry.getValue());
      }
   }

   private void printPIDInfo(final File logFile) throws IOException {
      if (!System.getProperty("os.name").startsWith("Windows") && !System.getProperty("os.name").startsWith("Mac")) {
         final int pid = Integer.parseInt(new File("/proc/self").getCanonicalFile().getName());
         LOG.debug("Process started: {} Used PIDs: {} Log to: {}", pid, getProcessCount(), logFile);
      }
   }
   
   public synchronized static int getProcessCount() {
      int count = -1;
      try {
         final Process process = new ProcessBuilder(new String[] { "bash", "-c", "ps -e -T | wc -l" }).start();
         final String result = StreamGobbler.getFullProcess(process, false).replaceAll("\n", "").replace("\r", "");
         count = Integer.parseInt(result.trim());
      } catch (IOException | NumberFormatException e) {

         e.printStackTrace();
      }
      return count;
   }
}
