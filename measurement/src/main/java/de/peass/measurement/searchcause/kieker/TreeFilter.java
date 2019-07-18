package de.peass.measurement.searchcause.kieker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.analysis.ModuleClassMapping;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.searchcause.data.CallTreeNode;
import kieker.analysis.IProjectContext;
import kieker.analysis.plugin.annotation.InputPort;
import kieker.analysis.plugin.annotation.Plugin;
import kieker.analysis.plugin.filter.AbstractFilterPlugin;
import kieker.common.configuration.Configuration;
import kieker.tools.traceAnalysis.systemModel.Execution;
import kieker.tools.traceAnalysis.systemModel.ExecutionTrace;
import kieker.tools.traceAnalysis.systemModel.Operation;

@Plugin(description = "A filter to transform generate PeASS-Call-trees")
public class TreeFilter extends AbstractFilterPlugin {

   private static final Logger LOG = LogManager.getLogger(TreeFilter.class);

   public static final String INPUT_EXECUTION_TRACE = "INPUT_EXECUTION_TRACE";

   private CallTreeNode root;

   private final TestCase test;

   public TreeFilter(final String prefix, final IProjectContext projectContext, TestCase test) {
      super(new Configuration(), projectContext);
      this.test = test;
   }

   @Override
   public Configuration getCurrentConfiguration() {
      return super.configuration;
   }

   public CallTreeNode getRoot() {
      return root;
   }

   public String getKiekerPattern(Operation operation) {
      final StringBuilder strBuild = new StringBuilder();
      for (final String t : operation.getSignature().getModifier()) {
         strBuild.append(t)
               .append(' ');
      }
      if (operation.getSignature().hasReturnType()) {
         strBuild.append(operation.getSignature().getReturnType())
               .append(' ');
      } else {
         strBuild.append("new")
               .append(' ');
      }
      strBuild.append(operation.getComponentType().getFullQualifiedName())
            .append('.');
      strBuild.append(operation.getSignature().getName()).append('(');

      boolean first = true;
      for (final String t : operation.getSignature().getParamTypeList()) {
         if (!first) {
            strBuild.append(',');
         } else {
            first = false;
         }
         strBuild.append(t);
      }
      strBuild.append(')');

      return strBuild.toString();
   }

   @InputPort(name = INPUT_EXECUTION_TRACE, eventTypes = { ExecutionTrace.class })
   public void handleInputs(final ExecutionTrace trace) {
      LOG.info("Trace: " + trace.getTraceId());

      CallTreeNode lastParent = null, lastAdded = null;
      int lastStackSize = 1;
      long testTraceId = -1;
      for (final Execution execution : trace.getTraceAsSortedExecutionSet()) {
         final String fullClassname = execution.getOperation().getComponentType().getFullQualifiedName().intern();
         final String methodname = execution.getOperation().getSignature().getName().intern();
         final String call = fullClassname + "#" + methodname;
         final String kiekerPattern = getKiekerPattern(execution.getOperation());
         LOG.trace(kiekerPattern);

         if (test.getClazz().equals(fullClassname) && test.getMethod().equals(methodname)) {
            root = new CallTreeNode(call, kiekerPattern, null);
            lastParent = root;
            testTraceId = execution.getTraceId();
         } else if (root != null && execution.getTraceId() == testTraceId) {
            LOG.trace(fullClassname + " " + execution.getOperation().getSignature() + " " + execution.getEoi() + " " + execution.getEss());
            LOG.trace("Last Stack: " + lastStackSize);
            if (execution.getEss() > lastStackSize) {
               lastParent = lastAdded;
               lastStackSize++;
            }
            while (execution.getEss() < lastStackSize) {
               lastParent = lastParent.getParent();
               lastStackSize--;
            }
            LOG.trace("Parent: " + lastParent.getCall());
            lastAdded = lastParent.append(call, kiekerPattern);
         }
      }
   }

}
