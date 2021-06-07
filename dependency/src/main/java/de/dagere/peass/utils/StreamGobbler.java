/**
 *     This file is part of PerAn.
 *
 *     PerAn is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PerAn is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PerAn.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.dagere.peass.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Combines the streams of a process.
 * 
 * @author reichelt
 *
 */
public class StreamGobbler extends Thread {

   private static final Logger LOG = LogManager.getLogger(StreamGobbler.class);

   private final InputStream is;
   private final boolean showOutput;
   private final StringBuffer output;

   private StreamGobbler(final InputStream is, final boolean showOutput, final StringBuffer output) {
      super("Gobbler");
      this.is = is;
      this.showOutput = showOutput;
      this.output = output;
   }

   @Override
   public void run() {
      try (final InputStreamReader isr = new InputStreamReader(is)){
         final BufferedReader br = new BufferedReader(isr);
         String line = null;
         while ((line = br.readLine()) != null) {
            if (output != null) {
               output.append(line);
               output.append("\n");
            }
            if (showOutput) {
               System.out.println(line);
            }
         }
         
      } catch (final IOException ioe) {
         ioe.printStackTrace();
      }
   }

   public static void showFullProcess(final Process process) {
      getFullProcess(process, true);
   }

   /**
    * Combines the streams of the process and eventually shows the output. Parallel calls to this method will lead to mixed outputs.
    * 
    * @param process The process that should be printed
    * @param showOutput Whether the output should be printed directly to System.out
    * @return The combined output of the streams of the process
    */
   public static String getFullProcess(final Process process, final boolean showOutput) {
      final StringBuffer output = new StringBuffer();
      final StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), showOutput, output);
      final StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), showOutput, output);

      outputGobbler.start();
      errorGobbler.start();

      LOG.trace("Waiting for process end");
      try {
         outputGobbler.join();
         errorGobbler.join();
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }
      LOG.trace("Process finished");
      return output.toString();
   }

}
