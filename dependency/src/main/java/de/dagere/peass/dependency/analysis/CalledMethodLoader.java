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
package de.dagere.peass.dependency.analysis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.kopeme.kieker.writer.onecall.OneCallReader;
import de.dagere.peass.config.KiekerConfig;
import de.dagere.peass.dependency.ClazzFileFinder;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.dependency.analysis.data.EntityUtil;
import de.dagere.peass.dependency.analysis.data.TraceElement;
import kieker.analysis.exception.AnalysisConfigurationException;

/**
 * Loads the methods that have been called from an given kieker-trace
 * 
 * @author reichelt
 *
 */
public class CalledMethodLoader {

   private static final Logger LOG = LogManager.getLogger(CalledMethodLoader.class);

   private final File kiekerTraceFolder;
   private final ModuleClassMapping mapping;
   private final KiekerConfig kiekerConfig;

   public CalledMethodLoader(final File kiekerTraceFolder, final ModuleClassMapping mapping, final KiekerConfig kiekerConfig) {
      this.kiekerTraceFolder = kiekerTraceFolder;
      this.mapping = mapping;
      this.kiekerConfig = kiekerConfig;
   }

   /**
    * Returns the calls of a kieker trace, i.e. all clazzes and their methods, that have been called at least once.
    * 
    * @param kiekerTraceFolder
    * @return
    */
   public Map<ChangedEntity, Set<String>> getCalledMethods() {
      try {
         if (kiekerConfig.isOnlyOneCallRecording()) {
            Map<ChangedEntity, Set<String>> calledMethodResult = loadMethods(); 
            return calledMethodResult;
         } else {
            final CalledMethodStage peassFilter = executePeassFilter(null);
            return peassFilter.getCalledMethods();
         }
      } catch (IllegalStateException | AnalysisConfigurationException e) {
         LOG.debug("Failed to load methods", e);
         e.printStackTrace();
      }
      return null;
   }

   private Map<ChangedEntity, Set<String>> loadMethods() {
      Set<String> calledMethods = OneCallReader.getCalledMethods(kiekerTraceFolder);
      Map<ChangedEntity, Set<String>> calledMethodResult = new HashMap<>();
      for (String calledMethod : calledMethods) {
         String methodNameWithoutModifiers = calledMethod.substring(calledMethod.lastIndexOf(' ')+1);
         ChangedEntity entity = EntityUtil.determineEntityWithDotSeparator(methodNameWithoutModifiers);
         
         final String outerClazzName = ClazzFileFinder.getOuterClass(entity.getClazz());
         final String moduleOfClass = mapping.getModuleOfClass(outerClazzName);
         
         ChangedEntity fullClassEntity = new ChangedEntity(entity.getClazz(), moduleOfClass);
         Set<String> currentMethodSet = calledMethodResult.get(fullClassEntity);
         if (currentMethodSet == null) {
            currentMethodSet = new HashSet<>();
            calledMethodResult.put(fullClassEntity, currentMethodSet);
         }
         currentMethodSet.add(entity.getMethod() + entity.getParameterString());
      }
      return calledMethodResult;
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

         LOG.debug("Size: {} ({}) Folder: {}", sizeInMB, size, kiekerTraceFolder);
         if (sizeInMB < kiekerConfig.getTraceSizeInMb()) {
            final CalledMethodStage peassFilter = executePeassFilter(prefix);
            return peassFilter.getCalls();
         } else {
            LOG.error("Trace size: {} MB - skipping", sizeInMB);
            return null;
         }
      } catch (IllegalStateException | AnalysisConfigurationException e) {
         LOG.debug("Failed to load trace", e);
         e.printStackTrace();
         return null;
      }
   }

   private CalledMethodStage executePeassFilter(final String prefix) throws AnalysisConfigurationException {
      CalledMethodStage peassStage = KiekerReader.getCalledMethodStage(kiekerTraceFolder, prefix, mapping);
      return peassStage;
   }

   public static void main(final String[] args) {
      final File kiekerTraceFile = new File(args[0]);
      KiekerConfig kiekerConfig = new KiekerConfig();
      kiekerConfig.setTraceSizeInMb(10000);
      final List<TraceElement> trace = new CalledMethodLoader(kiekerTraceFile, null, kiekerConfig).getShortTrace("");

      System.out.println("Trace-Size: " + trace.size());

   }

}
