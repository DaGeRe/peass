package de.peass.dependency.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.ClazzFileFinder;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TraceElement;
import kieker.analysis.trace.AbstractTraceProcessingStage;
import kieker.model.repository.SystemModelRepository;
import kieker.model.system.model.Execution;
import kieker.model.system.model.ExecutionTrace;

public class PeassStage extends AbstractTraceProcessingStage<ExecutionTrace> {

   private static final Logger LOG = LogManager.getLogger(PeassStage.class);

   private final Map<ChangedEntity, Set<String>> classes = new HashMap<>();
   private final ArrayList<TraceElement> calls = new ArrayList<>();
   private final String prefix;
   private final ModuleClassMapping mapping;

   private final static int CALLCOUNT = 10000000;

   public PeassStage(final SystemModelRepository systemModelRepository, final String prefix, final ModuleClassMapping mapping) {
      super(systemModelRepository);
      this.prefix = prefix;
      this.mapping = mapping;
   }

   @Override
   protected void execute(final ExecutionTrace trace) throws Exception {
      LOG.info("Trace: " + trace.getTraceId());

      for (final Execution execution : trace.getTraceAsSortedExecutionSet()) {
         if (calls.size() > CALLCOUNT) {
            calls.add(new TraceElement("ATTENTION", "TO MUCH CALLS", 0));
            LOG.info("Trace Reading Aborted, Cause More Than " + CALLCOUNT + " Appeared");
            break;
         }

         final String fullClassname = execution.getOperation().getComponentType().getFullQualifiedName().intern();
         if ((prefix == null || prefix != null && fullClassname.startsWith(prefix))
               && !fullClassname.contains("junit") && !fullClassname.contains("log4j")
               && !fullClassname.equals("de.peass.generated.GeneratedTest")) {
            final String methodname = execution.getOperation().getSignature().getName().intern();

            // KoPeMe-methods are not relevant
            if (!methodname.equals("logFullData")
                  && !methodname.equals("useKieker")
                  && !methodname.equals("getWarmupExecutions")
                  && !methodname.equals("getExecutionTimes")
                  && !methodname.equals("getMaximalTime")
                  && !methodname.equals("getRepetitions")
                  && !methodname.equals("getDataCollectors")) {
               final TraceElement traceelement = buildTraceElement(execution, fullClassname, methodname);

               calls.add(traceelement);
               // final String clazzFilename = ClazzFinder.getClassFilename(projectFolder, fullClassname);
               final String outerClazzName = ClazzFileFinder.getOuterClass(fullClassname);
               final String moduleOfClass = mapping.getModuleOfClass(outerClazzName);
               final ChangedEntity fullClassEntity = new ChangedEntity(fullClassname, moduleOfClass);
               traceelement.setModule(moduleOfClass);
               Set<String> currentMethodSet = classes.get(fullClassEntity);
               if (currentMethodSet == null) {
                  currentMethodSet = new HashSet<>();
                  classes.put(fullClassEntity, currentMethodSet);
               }
               currentMethodSet.add(methodname);
            }
         }
      }
      LOG.info("Finished");
   }

   private TraceElement buildTraceElement(final Execution execution, final String fullClassname, final String methodname) {
      final TraceElement traceelement = new TraceElement(fullClassname, methodname, execution.getEss());
      if (Arrays.asList(execution.getOperation().getSignature().getModifier()).contains("static")) {
         traceelement.setStatic(true);
      }
      final String[] paramTypeList = execution.getOperation().getSignature().getParamTypeList();
      LOG.info("Parameters " + fullClassname + " " + methodname + " " + Arrays.toString(paramTypeList));
      LOG.debug(paramTypeList.length); // TODO delete
      final String[] internParamTypeList = getInternTypeList(paramTypeList);
      traceelement.setParameterTypes(internParamTypeList);
      return traceelement;
   }

   public static String[] getInternTypeList(final String[] paramTypeList) {
      List<String> parameters = new LinkedList<>();
      int remainingOpen = 0;
      for (int i = 0; i < paramTypeList.length; i++) {
         if (remainingOpen > 0) {
            int openCount = StringUtils.countMatches(paramTypeList[i], '<');
            int closeCount = StringUtils.countMatches(paramTypeList[i], '>');
            remainingOpen += openCount - closeCount;
            String appendedParameter = parameters.get(parameters.size() - 1) + paramTypeList[i];
            parameters.set(parameters.size() - 1, appendedParameter);
         } else {
            if (paramTypeList[i].contains("<")) {
               int openCount = StringUtils.countMatches(paramTypeList[i], '<');
               int closeCount = StringUtils.countMatches(paramTypeList[i], '>');
               remainingOpen = openCount - closeCount;
               parameters.add(paramTypeList[i]);
            } else {
               parameters.add(paramTypeList[i].intern());
            }
         }
      }
      final String[] internParamTypeList = parameters.toArray(new String[0]);
      return internParamTypeList;
   }

   public ArrayList<TraceElement> getCalls() {
      return calls;
   }

   public Map<ChangedEntity, Set<String>> getCalledMethods() {
      return classes;
   }
}
