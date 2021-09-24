package net.kieker.sourceinstrumentation;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.utils.StreamGobbler;

public class JavaVersionUtil {
   private static final Logger LOG = LogManager.getLogger(JavaVersionUtil.class);
   
   public static int getSystemJavaVersion() {
      try {
         Process process = Runtime.getRuntime().exec("javac -version");
         String output = StreamGobbler.getFullProcess(process, false);
         if (output.startsWith("javac ")) {
            String versionString = output.split(" ")[1];
            return getJavaVersion(versionString);
         } else {
            LOG.error("javac -version returned unexpected output; javac should be installed for source instrumentation");
            LOG.error(output);
            return -1;
         }
      } catch (IOException e) {
         e.printStackTrace();
         return -1;
      }

   }

   private static int getJavaVersion(String version) {
      if (version.startsWith("1.")) {
         version = version.substring(2, 3);
      } else {
         int dot = version.indexOf(".");
         if (dot != -1) {
            version = version.substring(0, dot);
         }
      }
      return Integer.parseInt(version);
   }
}
