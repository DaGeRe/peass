package de.dagere.peass.measurement.rca.kieker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.ClazzFileFinder;
import de.dagere.peass.dependency.analysis.ModuleClassMapping;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import kieker.analysis.trace.AbstractTraceProcessingStage;
import kieker.model.repository.SystemModelRepository;
import kieker.model.system.model.Execution;
import kieker.model.system.model.ExecutionTrace;

public class TreeStage extends AbstractTraceProcessingStage<ExecutionTrace> {

   private static final Logger LOG = LogManager.getLogger(TreeStage.class);

   private CallTreeNode root;

   private MeasurementConfig measurementConfig;

   private final TestCase test;

   private final boolean ignoreEOIs;
   private final ModuleClassMapping mapping;

   public TreeStage(final SystemModelRepository systemModelRepository, final TestCase test, final boolean ignoreEOIs, final MeasurementConfig config,
         final ModuleClassMapping mapping) {
      super(systemModelRepository);

      this.test = test;
      this.measurementConfig = config;
      this.ignoreEOIs = ignoreEOIs;
      this.mapping = mapping;
   }

   public CallTreeNode getRoot() {
      return root;
   }

   private CallTreeNode lastParent = null, lastAdded = null;
   private int lastStackSize = 1;
   private long testTraceId = -1;

   @Override
   protected void execute(final ExecutionTrace trace) throws Exception {
      LOG.info("Trace: " + trace.getTraceId());

      for (final Execution execution : trace.getTraceAsSortedExecutionSet()) {
         final String fullClassname = execution.getOperation().getComponentType().getFullQualifiedName().intern();
         final String methodname = execution.getOperation().getSignature().getName().intern();
         final String call = fullClassname + "#" + methodname;
         final String kiekerPattern = KiekerPatternConverter.getKiekerPattern(execution.getOperation());
         LOG.debug("{} {}", kiekerPattern, execution.getEss());

         // ignore synthetic java methods
         if (!methodname.equals("class$") && !methodname.startsWith("access$")) {
            addExecutionToTree(execution, fullClassname, methodname, call, kiekerPattern);
         }
      }
   }

   private void addExecutionToTree(final Execution execution, final String fullClassname, final String methodname, final String call, final String kiekerPattern) {
      if (test.getClazz().equals(fullClassname) && test.getMethod().equals(methodname)) {
         readRoot(execution, call, kiekerPattern);
         setModule(fullClassname, root);
      } else if (root != null && execution.getTraceId() == testTraceId) {
         LOG.trace(fullClassname + " " + execution.getOperation().getSignature() + " " + execution.getEoi() + " " + execution.getEss());
         LOG.trace("Last Stack: " + lastStackSize);

         callLevelDown(execution);
         callLevelUp(execution);
         LOG.trace("Parent: {} {}", lastParent.getCall(), lastParent.getEss());

         if (execution.getEss() == lastParent.getEss()) {
            final String message = "Trying to add " + call + "(" + execution.getEss() + ")" + " to " + lastParent.getCall() + "(" + lastParent.getEss()
                  + "), but parent ess always needs to be child ess -1";
            LOG.error(message);
            throw new RuntimeException(message);
         }

         boolean hasEqualNode = false;
         for (CallTreeNode candidate : lastParent.getChildren()) {
            if (candidate.getKiekerPattern().equals(kiekerPattern)) {
               hasEqualNode = true;
               lastAdded = candidate;
            }
         }
         if (!ignoreEOIs || !hasEqualNode) {
            lastAdded = lastParent.appendChild(call, kiekerPattern, null);
            setModule(fullClassname, lastAdded);
         }
      }
   }

   private void setModule(final String fullClassname, final CallTreeNode node) {
      final String outerClazzName = ClazzFileFinder.getOuterClass(fullClassname);
      final String moduleOfClass = mapping.getModuleOfClass(outerClazzName);
      node.setModule(moduleOfClass);
   }

   private void callLevelUp(final Execution execution) {
      while (execution.getEss() < lastStackSize) {
         LOG.trace("Level up: " + execution.getEss() + " " + lastStackSize);
         lastParent = lastParent.getParent();
         lastStackSize--;
      }
   }

   private void callLevelDown(final Execution execution) {
      if (execution.getEss() > lastStackSize) {
         LOG.trace("Level down: " + execution.getEss() + " " + lastStackSize);
         lastParent = lastAdded;
         // lastStackSize++;
         if (lastStackSize + 1 != lastParent.getEss() + 1) {
            LOG.error("Down caused wrong lastStackSize: {} {}", lastStackSize, lastParent.getEss());
         }
         lastStackSize = lastParent.getEss() + 1;
         LOG.trace("Stack size after going down: {} Measured: {}", lastParent.getEss(), lastStackSize);
      }
   }

   private void readRoot(final Execution execution, final String call, final String kiekerPattern) {
      if (kiekerPattern.startsWith("public ")) {
         root = new CallTreeNode(call, kiekerPattern, null, measurementConfig);
      } else {
         root = new CallTreeNode(call, "public " + kiekerPattern, null, measurementConfig);
      }
      lastParent = root;
      testTraceId = execution.getTraceId();
   }

}
