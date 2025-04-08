package de.dagere.peass.measurement.utils.sjsw;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kieker.KiekerPatternConverter;
import de.dagere.peass.measurement.rca.treeanalysis.TreeUtil;
import io.github.terahidro2003.cct.result.StackTraceTreeNode;
import io.github.terahidro2003.cct.result.VmMeasurement;

public class SjswCctConverter {
   private static final Logger LOG = LogManager.getLogger(SjswCctConverter.class);

   private final String commit, predecessor;
   private final MeasurementConfig config;

   public SjswCctConverter(String commit, String predecessor, MeasurementConfig config) {
      this.commit = commit;
      this.predecessor = predecessor;
      this.config = config;

      if (commit == null && predecessor == null) {
         throw new IllegalArgumentException("Commit and Predesseror cannot be null");
      }
   }

   public CallTreeNode convertToCCT(final StackTraceTreeNode currentBAT, final StackTraceTreeNode predecessorBAT) {
      final String methodNameWithNew = normalizeKiekerPattern(currentBAT);
      final String call = KiekerPatternConverter.getCall(methodNameWithNew);

      CallTreeNode root = new CallTreeNode(call, methodNameWithNew, methodNameWithNew, config);
      CallTreeNode otherNode = new CallTreeNode(call, methodNameWithNew, methodNameWithNew, config);

      root.setOtherCommitNode(otherNode);
      otherNode.setOtherCommitNode(root);

      buildPeassNodeStatistics(currentBAT, predecessorBAT, root.getOtherCommitNode());

      appendAllChildren(currentBAT, predecessorBAT, root, otherNode);

      convertCallContextTreeToCallTree(currentBAT, predecessorBAT, root.getOtherCommitNode());

      return root;
   }

   private void appendAllChildren(final StackTraceTreeNode currentBAT, final StackTraceTreeNode predecessorBAT, CallTreeNode root, CallTreeNode otherNode) {
      if (currentBAT != null) {
         for (StackTraceTreeNode child : currentBAT.getChildren()) {
            final String methodNameWithNewChild = normalizeKiekerPattern(child);
            final String callChild = KiekerPatternConverter.getCall(methodNameWithNewChild);
            root.appendChild(callChild, methodNameWithNewChild, methodNameWithNewChild);
         }
      }

      if (predecessorBAT != null) {
         for (StackTraceTreeNode child : predecessorBAT.getChildren()) {
            final String methodNameWithNewChild = normalizeKiekerPattern(child);
            final String callChild = KiekerPatternConverter.getCall(methodNameWithNewChild);
            otherNode.appendChild(callChild, methodNameWithNewChild, methodNameWithNewChild);
         }
      }

      TreeUtil.findChildMapping(root, otherNode);
   }

   private void convertCallContextTreeToCallTree(final StackTraceTreeNode currentBAT,
         final StackTraceTreeNode predecessorBAT, final CallTreeNode parentNode) {
      LOG.info("Current original node: {}", currentBAT != null ? currentBAT.getPayload().getMethodName() : null);
      LOG.info("Other original node: {}", predecessorBAT != null ? predecessorBAT.getPayload().getMethodName() : null);
      LOG.info("Original node: {}", predecessorBAT != null ? predecessorBAT.getPayload().getMethodName() : null);

      for (CallTreeNode childNode : parentNode.getChildren()) {
         StackTraceTreeNode childCurrentStack = findChild(currentBAT, childNode.getOtherCommitNode());
         StackTraceTreeNode childPredecessorStack = findChild(predecessorBAT, childNode);
         
         LOG.info("Adding data for " +
               (childCurrentStack != null ? childCurrentStack.getPayload().getMethodName() : null)
               + " " +
               (childPredecessorStack != null ? childPredecessorStack.getPayload().getMethodName() : null));

         buildPeassNodeStatistics(childCurrentStack, childPredecessorStack, childNode);

         appendAllChildren(childCurrentStack, childPredecessorStack, childNode.getOtherCommitNode(), childNode);
         convertCallContextTreeToCallTree(childCurrentStack, childPredecessorStack, childNode);
      }
   }

   private StackTraceTreeNode findChild(final StackTraceTreeNode stackTraceParent, CallTreeNode childNode) {
      StackTraceTreeNode childCurrentStack = null;
      if (stackTraceParent != null) {
         for (StackTraceTreeNode stackTraceChild : stackTraceParent.getChildren()) {
            String kiekerPattern = normalizeKiekerPattern(stackTraceChild);
            if (kiekerPattern.equals(childNode.getKiekerPattern())) {
               childCurrentStack = stackTraceChild;
            }
         }
      }
      return childCurrentStack;
   }

   private static String normalizeKiekerPattern(StackTraceTreeNode node) {
      String methodSignature = node.getPayload().getMethodName();
      if ("root".equals(methodSignature)) {
         methodSignature = "RootClass.root";
      }
      if (!methodSignature.contains("(")) {
         methodSignature = methodSignature + "()";
      } else {
         int indexOfParenthesis = methodSignature.indexOf('(');
         String partBeforeParenthesis = methodSignature.substring(0, indexOfParenthesis);
         String parameters = methodSignature.substring(indexOfParenthesis).replace(" ", "");
         methodSignature = partBeforeParenthesis + parameters;
      }

      // TODO That won't work in all cases, since the new needs to be behind the modifiers - but leaving it for now
      if (methodSignature.contains("<init>")) {
         methodSignature = "new " + methodSignature;
      }
      return methodSignature;
   }

   private void buildPeassNodeStatistics(StackTraceTreeNode node, StackTraceTreeNode otherNode, final CallTreeNode peassNode) {
      if (peassNode.getData() != null &&
            (peassNode.getData().get(commit) != null || peassNode.getData().get(predecessor) != null)) {
         throw new RuntimeException("Tried to add data twice to " + peassNode.getCall());
      }
      LOG.info("Creating peass node for stacktracetreenodes: {} -> {}", 
            node != null ? (node.getPayload().getMethodName() + "(" + node.getMeasurements() + ")" ) : null,
            otherNode != null ? otherNode.getPayload().getMethodName() + "(" + otherNode.getMeasurements() + ")" : null);
      LOG.info("Peass node: " + peassNode);
      peassNode.initCommitData();

      if (config.isUseIterativeSampling()) {
         if (node != null) {
            addIterativeMeasurements(commit, node, peassNode, config.getVms(), config.getIterations());
         }
         if (otherNode != null) {
            LOG.info("Adding measurements for the other commit {}", predecessor);
            addIterativeMeasurements(predecessor, otherNode, peassNode, config.getVms(), config.getIterations());
         }
      } else {
         if (node != null) {
            addMeasurements(commit, node, peassNode, config.getVms());
         }
         if (otherNode != null) {
            LOG.info("Adding measurements for the other commit {}", predecessor);
            addMeasurements(predecessor, otherNode, peassNode, config.getVms());
         }
      }
      peassNode.createStatistics(commit);
      peassNode.createStatistics(predecessor);

      LOG.info("Current stats: {} --> {}", commit, peassNode.getData().get(commit).getResults().size());
      if (otherNode != null) {
         LOG.info("Current stats: {} --> {}", predecessor, peassNode.getData().get(predecessor).getResults().size());
      }
   }

   public static String getCorrectCallString(String method) {
      int lastParenthesisIndex = method.contains("(") ? method.lastIndexOf("(") : method.length() - 1;
      String methodName = method.substring(0, lastParenthesisIndex);

      String[] parts = methodName.split(" ");
      String methodNameWithoutType = parts.length > 1 ? parts[parts.length - 1] : method;

      int lastDotIndex = methodNameWithoutType.contains(".") ? methodNameWithoutType.lastIndexOf(".")
            : -1;
      String className = lastDotIndex > 0 ? methodNameWithoutType.substring(0, lastDotIndex) : "";
      methodName = methodNameWithoutType.substring(lastDotIndex + 1);

      return className + "#" + methodName;
   }

   private static void addMeasurements(String commit, StackTraceTreeNode node, CallTreeNode peassNode, int vms) {
      List<Double> measurementsForSpecificCommit = node.getMeasurements().get(commit);
      if (measurementsForSpecificCommit == null || measurementsForSpecificCommit.isEmpty()) {
         throw new IllegalArgumentException("Possibly invalid measurement data. Commit " +
               commit + " does not contain any measurement data.");
      }

      if (measurementsForSpecificCommit.size() != vms) {
         int missing = vms - measurementsForSpecificCommit.size();
         for (int i = 0; i < missing; i++) {
            measurementsForSpecificCommit.add(0.0);
         }
      }

      for (int vm = 0; vm < vms; vm++) {
         peassNode.initVMData(commit);
         double measurement = measurementsForSpecificCommit.get(vm);
         peassNode.addMeasurement(commit, (long) measurement);
      }
   }

   private static void addIterativeMeasurements(final String commit, final StackTraceTreeNode node, final CallTreeNode peassNode,
         final int vms, final int iterations) {
      List<VmMeasurement> measurementsForSpecificCommit = node.getVmMeasurements().get(commit);
      if (measurementsForSpecificCommit == null || measurementsForSpecificCommit.isEmpty()) {
         throw new IllegalArgumentException("Possibly invalid iterative measurement data. Commit " +
               commit + " does not contain any measurement data.");
      }

      if (measurementsForSpecificCommit.size() != vms) {
         LOG.error("Amount of measurements ({}) is not equal to the amount of VMs ({}).", measurementsForSpecificCommit.size(), vms);
      }

      for (int vm = 0; vm < vms; vm++) {
         final int vmfinal = vm;
         List<VmMeasurement> vmMeasurements = measurementsForSpecificCommit.stream().filter(vmm -> vmm.getVm() == vmfinal).collect(Collectors.toList());
         if (vmMeasurements.isEmpty() || vmMeasurements.get(0) == null) {
            LOG.warn("No measurements found for VM {}", vm);
            return;
         }
         final List<Double> measurements = vmMeasurements.get(0).getMeasurements();
         LOG.debug("Call: {} VM: {} vmMeasurements: {} measurements: {}", peassNode.getCall(),
               vm, vmMeasurements.size(), measurements.size());
         if (measurements.size() > 1) {
            final List<StatisticalSummary> values = new LinkedList<>();
            int sliceSize = Math.max(measurements.size() / iterations, 1);
            LOG.info("Measurements: {} Iterations: {} Slice size: {}", measurements.size(), iterations, sliceSize);
            List<List<Double>> slicedIterationMeasurements = Lists.partition(measurements, sliceSize);
            slicedIterationMeasurements.forEach(slice -> {
               final SummaryStatistics statistic = new SummaryStatistics();
               slice.forEach(measurement -> {
                  statistic.addValue((long) (double) measurement);
                  LOG.info("Adding sjsw measurement: {}", measurement);
               });
               values.add(statistic);
            });
            LOG.debug("Adding to {} - Values: {} ", peassNode.getCall(), values.size());
            peassNode.addAggregatedMeasurement(commit, values);
         } else {
            peassNode.initVMData(commit);
            measurements.forEach(measurement -> {
               peassNode.addMeasurement(commit, (long) (double) measurement);
            });
         }
      }
   }
}
