package de.peass.measurement.rca.kieker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.rca.data.CallTreeNode;
import kieker.analysis.IProjectContext;
import kieker.analysis.plugin.annotation.InputPort;
import kieker.analysis.plugin.annotation.Plugin;
import kieker.analysis.plugin.filter.AbstractFilterPlugin;
import kieker.common.configuration.Configuration;
import kieker.tools.trace.analysis.systemModel.Execution;
import kieker.tools.trace.analysis.systemModel.ExecutionTrace;

@Plugin(description = "A filter to transform generate PeASS-Call-trees")
public class TreeFilter extends AbstractFilterPlugin {

   private static final Logger LOG = LogManager.getLogger(TreeFilter.class);

   public static final String INPUT_EXECUTION_TRACE = "INPUT_EXECUTION_TRACE";

   private CallTreeNode root;

   private final TestCase test;

   private final boolean ignoreEOIs;

   public TreeFilter(final String prefix, final IProjectContext projectContext, final TestCase test, boolean ignoreEOIs) {
      super(new Configuration(), projectContext);
      this.test = test;
      this.ignoreEOIs = ignoreEOIs;
   }

   @Override
   public Configuration getCurrentConfiguration() {
      return super.configuration;
   }

   public CallTreeNode getRoot() {
      return root;
   }

   CallTreeNode lastParent = null, lastAdded = null;
   int lastStackSize = 1;
   long testTraceId = -1;

   @InputPort(name = INPUT_EXECUTION_TRACE, eventTypes = { ExecutionTrace.class })
   public void handleInputs(final ExecutionTrace trace) {
      LOG.info("Trace: " + trace.getTraceId());

      for (final Execution execution : trace.getTraceAsSortedExecutionSet()) {
         final String fullClassname = execution.getOperation().getComponentType().getFullQualifiedName().intern();
         final String methodname = execution.getOperation().getSignature().getName().intern();
         final String call = fullClassname + "#" + methodname;
         final String kiekerPattern = KiekerPatternConverter.getKiekerPattern(execution.getOperation());
         LOG.trace(kiekerPattern);

         // ignore synthetic java methods
         if (!methodname.equals("class$") && !methodname.startsWith("access$")) {
            addExecutionToTree(execution, fullClassname, methodname, call, kiekerPattern);
         }
      }
   }

   private void addExecutionToTree(final Execution execution, final String fullClassname, final String methodname, final String call, final String kiekerPattern) {
      if (test.getClazz().equals(fullClassname) && test.getMethod().equals(methodname)) {
         readRoot(execution, call, kiekerPattern);
      } else if (root != null && execution.getTraceId() == testTraceId) {
         LOG.trace(fullClassname + " " + execution.getOperation().getSignature() + " " + execution.getEoi() + " " + execution.getEss());
         LOG.trace("Last Stack: " + lastStackSize);
         callLevelDown(execution);
         callLevelUp(execution);
         LOG.trace("Parent: " + lastParent.getCall());
         boolean hasEqualNode = false;
         for (CallTreeNode candidate : lastParent.getChildren()) {
            if (candidate.getKiekerPattern().equals(kiekerPattern)) {
               hasEqualNode = true;
            }
         }
         if (!ignoreEOIs || !hasEqualNode) {
            lastAdded = lastParent.appendChild(call, kiekerPattern, null);
         }
      }
   }

   private void callLevelUp(final Execution execution) {
      while (execution.getEss() < lastStackSize) {
         lastParent = lastParent.getParent();
         lastStackSize--;
      }
   }

   private void callLevelDown(final Execution execution) {
      if (execution.getEss() > lastStackSize) {
         lastParent = lastAdded;
         lastStackSize++;
      }
   }

   private void readRoot(final Execution execution, final String call, final String kiekerPattern) {
      root = new CallTreeNode(call, kiekerPattern, null);
      lastParent = root;
      testTraceId = execution.getTraceId();
   }

}
