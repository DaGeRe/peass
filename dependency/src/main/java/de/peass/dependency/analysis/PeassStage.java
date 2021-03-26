package de.peass.dependency.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
      final String[] internParamTypeList = new String[paramTypeList.length];
      for (int i = 0; i < paramTypeList.length; i++) {
         internParamTypeList[i] = paramTypeList[i].intern();
      }
      traceelement.setParameterTypes(paramTypeList);
      return traceelement;
   }

   public ArrayList<TraceElement> getCalls() {
      return calls;
   }

   public Map<ChangedEntity, Set<String>> getCalledMethods() {
      return classes;
   }
}
