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
package de.peass.dependency.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TraceElement;
import kieker.analysis.AnalysisController;
import kieker.analysis.exception.AnalysisConfigurationException;
import kieker.common.configuration.Configuration;
import kieker.tools.trace.analysis.filter.traceReconstruction.TraceReconstructionFilter;

/**
 * Loads the methods that have been called from an given kieker-trace
 * 
 * @author reichelt
 *
 */
public class CalledMethodLoader {

   public static final int TRACE_MAX_SIZE = 50;

   private static final PrintStream realSystemOut = System.out;
   private static final PrintStream realSystemErr = System.err;

   private static final Logger LOG = LogManager.getLogger(CalledMethodLoader.class);

//   private TraceReconstructionFilter traceReconstructionFilter;
//   private final AnalysisController analysisController = new AnalysisController();
   private final File kiekerTraceFolder;
   private final ModuleClassMapping mapping;

   public CalledMethodLoader(final File kiekerTraceFolder, final ModuleClassMapping mapping) {
      this.kiekerTraceFolder = kiekerTraceFolder;
      this.mapping = mapping;
   }

   /**
    * Returns the calls of a kieker trace, i.e. all clazzes and their methods, that have been called at least once.
    * 
    * @param kiekerTraceFolder
    * @return
    */
   public Map<ChangedEntity, Set<String>> getCalledMethods(final File kiekerOutputFile) {
      Map<ChangedEntity, Set<String>> calledClasses = null;
      try {
         System.setOut(new PrintStream(kiekerOutputFile));
         System.setErr(new PrintStream(kiekerOutputFile));
         final PeASSFilter peassFilter = executePeassFilter(null);
         return peassFilter.getCalledMethods();
      } catch (IllegalStateException | AnalysisConfigurationException | FileNotFoundException e) {
         e.printStackTrace();
      } finally {
         System.setOut(realSystemOut);
         System.setErr(realSystemErr);
      }
      return calledClasses;
   }

   /**
    * Returns all method executions of the trace in their order of execution.
    * 
    * @param kiekerTraceFolder
    * @param prefix
    * @return
    */
   public ArrayList<TraceElement> getShortTrace(final String prefix) {
      try {
         final long size = FileUtils.sizeOfDirectory(kiekerTraceFolder);
         final long sizeInMB = size / (1024 * 1024);

         LOG.debug("Größe: {} ({}) Ordner: {}", sizeInMB, size, kiekerTraceFolder);
         if (sizeInMB < TRACE_MAX_SIZE) {
            final PeASSFilter peassFilter = executePeassFilter(prefix);
            return peassFilter.getCalls();
         } else {
            LOG.error("Trace size: {} MB - skipping", sizeInMB);
            return null;
         }
      } catch (IllegalStateException | AnalysisConfigurationException e) {
         e.printStackTrace();
         return null;
      }
   }

   private PeASSFilter executePeassFilter(final String prefix) throws AnalysisConfigurationException {
      KiekerReader reader = new KiekerReader(kiekerTraceFolder);
      reader.initBasic();
      TraceReconstructionFilter traceReconstructionFilter = reader.initTraceReconstruction();
      
      AnalysisController analysisController = reader.getAnalysisController();

      // TODO In case of error, logging for TraceReconstructionFilter should be disabled, since this produces huge traces
      // and errors are expected (if calls get a timeout)
      final PeASSFilter kopemeFilter = new PeASSFilter(prefix, new Configuration(), analysisController, mapping);
      analysisController.connect(traceReconstructionFilter, TraceReconstructionFilter.OUTPUT_PORT_NAME_EXECUTION_TRACE,
            kopemeFilter, PeASSFilter.INPUT_EXECUTION_TRACE);

      analysisController.run();
      return kopemeFilter;
   }

   public static void main(final String[] args) {
      final File kiekerTraceFile = new File(args[0]);
      final List<TraceElement> trace = new CalledMethodLoader(kiekerTraceFile, null).getShortTrace("");

      System.out.println("Trace-Size: " + trace.size());

   }

}
