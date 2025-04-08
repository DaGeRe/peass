package de.dagere.peass.measurement.utils.sjsw;

import com.google.common.collect.Lists;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kieker.KiekerPatternConverter;
import io.github.terahidro2003.cct.result.StackTraceTreeNode;
import io.github.terahidro2003.cct.result.VmMeasurement;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class SjswCctConverter {
    private static final Logger LOG = LogManager.getLogger(SjswCctConverter.class);

    public static CallTreeNode convertCallContextTreeToCallTree(final StackTraceTreeNode currentBAT,
          final StackTraceTreeNode predecessorBAT, CallTreeNode ctn,
          final String commit, final String predecessor, final MeasurementConfig config) {
        if (commit == null && predecessor == null) {
            throw new IllegalArgumentException("Commit and Predesseror cannot be null");
        }

        LOG.info("Current original node: {}", currentBAT.getPayload().getMethodName());

        final StackTraceTreeNode otherNode = predecessorBAT != null ? search(currentBAT, predecessorBAT) : null;

        LOG.info("Other original node: {}", otherNode != null ? otherNode.getPayload().getMethodName() : null);
        LOG.info("Original node: {}", predecessorBAT != null ? predecessorBAT.getPayload().getMethodName() : null);

        final String methodNameWithNew = normalizeKiekerPattern(currentBAT);
        final String call = KiekerPatternConverter.getCall(methodNameWithNew);
        if(ctn == null) {
            ctn = new CallTreeNode(call,
                    methodNameWithNew,
                    methodNameWithNew,
                    config);
        } else {
            appendChild(currentBAT, ctn);
            ctn = ctn.getChildByKiekerPattern(methodNameWithNew);
        }
        
        if (otherNode != null) {
            CallTreeNode otherCallTreeNode = createOtherNodeRecursive(otherNode, currentBAT, null, commit, predecessor, config);
            ctn.setOtherCommitNode(otherCallTreeNode);
        }

        List<StackTraceTreeNode> children = currentBAT.getChildren();
//        if (children.isEmpty() && ctn != null) {
//            LOG.info("Analyzing child");
//            createPeassNode(currentBAT, otherNode, ctn, commit, predecessor, true, config);
//        }
        for (StackTraceTreeNode child : children) {
           convertCallContextTreeToCallTree(child, otherNode, ctn, commit, predecessor, config);
        }

        buildPeassNodeStatistics(currentBAT, otherNode, ctn, commit, predecessor, config);

        return ctn;
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
        
        //TODO That won't work in all cases, since the new needs to be behind the modifiers - but leaving it for now
        if (methodSignature.contains("<init>")) {
           methodSignature = "new " + methodSignature;
       }
        return methodSignature;
    }

    public static StackTraceTreeNode search(StackTraceTreeNode searchable, StackTraceTreeNode tree) {
        Stack<StackTraceTreeNode> stack = new Stack<>();
        stack.push(tree);

        while (!stack.isEmpty()) {
            StackTraceTreeNode currentNode = stack.pop();

            if(searchable.equals(currentNode)) {
                return currentNode;
            }

            for (StackTraceTreeNode child : currentNode.getChildren()) {
                if (child != null) {
                    stack.push(child);
                }
            }
        }

        return null;
    }

    private static void buildPeassNodeStatistics(StackTraceTreeNode node, StackTraceTreeNode otherNode, final CallTreeNode peassNode,
                                        String commit, String oldCommit,
                                        MeasurementConfig config) {
        
        if (peassNode.getData() != null && 
              (peassNode.getData().get(commit) != null || peassNode.getData().get(oldCommit) != null)) {
           throw new RuntimeException("Tried to add data twice to " + peassNode.getCall());
        }
        LOG.info("Creating peass node for stacktracetreenodes: {} -> {}", node.getPayload().getMethodName() + "(" + node.getMeasurements() + ")", otherNode != null ? otherNode.getPayload().getMethodName() + "(" + otherNode.getMeasurements() + ")" : null);
        LOG.info("Peass node: " + peassNode);
        peassNode.initCommitData();

        if (config.isUseIterativeSampling()) {
            addIterativeMeasurements(commit, node, peassNode, config.getVms(), config.getIterations());
            if(otherNode != null) {
                LOG.info("Adding measurements for the other commit {}", oldCommit);
                addIterativeMeasurements(oldCommit, otherNode, peassNode, config.getVms(), config.getIterations());
            }
        } else {
            addMeasurements(commit, node, peassNode, config.getVms());
            if(otherNode != null) {
                LOG.info("Adding measurements for the other commit {}", oldCommit);
                addMeasurements(oldCommit, otherNode, peassNode, config.getVms());
            }
        }
        peassNode.createStatistics(commit);
        peassNode.createStatistics(oldCommit);

        LOG.info("Current stats: {} --> {}", commit, peassNode.getData().get(commit).getResults().size());
        if(otherNode != null) {
            LOG.info("Current stats: {} --> {}", oldCommit, peassNode.getData().get(oldCommit).getResults().size());
        }
    }

    public static String getCorrectCallString(String method) {
        int lastParenthesisIndex = method.contains("(") ? method.lastIndexOf("(") : method.length() -1;
        String methodName = method.substring(0, lastParenthesisIndex);

        String[] parts = methodName.split(" ");
        String methodNameWithoutType = parts.length > 1 ? parts[parts.length - 1] : method;

        int lastDotIndex = methodNameWithoutType.contains(".") ? methodNameWithoutType.lastIndexOf(".")
                : -1;
        String className = lastDotIndex > 0 ? methodNameWithoutType.substring(0, lastDotIndex) : "";
        methodName = methodNameWithoutType.substring(lastDotIndex + 1);

        return className + "#" + methodName;
    }

    private static void appendChild(StackTraceTreeNode node, CallTreeNode peassNode) {
        // check is done as a workaround for Peass kieker pattern check
        String methodNameWithNew = normalizeKiekerPattern(node);
        String call = KiekerPatternConverter.getCall(methodNameWithNew);
        peassNode.appendChild(call, methodNameWithNew, methodNameWithNew);
    }

    private static void addMeasurements(String commit, StackTraceTreeNode node, CallTreeNode peassNode, int vms) {
        List<Double> measurementsForSpecificCommit = node.getMeasurements().get(commit);
        if(measurementsForSpecificCommit == null || measurementsForSpecificCommit.isEmpty()) {
            throw new IllegalArgumentException("Possibly invalid measurement data. Commit " +
                    commit + " does not contain any measurement data.");
        }

        if (measurementsForSpecificCommit.size() != vms) {
            int missing = vms - measurementsForSpecificCommit.size();
            for (int i = 0; i<missing; i++) {
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
        if(measurementsForSpecificCommit == null || measurementsForSpecificCommit.isEmpty()) {
            throw new IllegalArgumentException("Possibly invalid iterative measurement data. Commit " +
                    commit + " does not contain any measurement data.");
        }

        if (measurementsForSpecificCommit.size() != vms) {
            LOG.error("Amount of measurements ({}) is not equal to the amount of VMs ({}).", measurementsForSpecificCommit.size(), vms);
        }

        for (int vm = 0; vm < vms; vm++) {
            final int vmfinal = vm;
            List<VmMeasurement> vmMeasurements = measurementsForSpecificCommit.stream().filter(vmm -> vmm.getVm() == vmfinal).collect(Collectors.toList());
            if(vmMeasurements.isEmpty() || vmMeasurements.get(0) == null) {
                LOG.warn("No measurements found for VM {}", vm);
                return;
            }
            final List<Double> measurements = vmMeasurements.get(0).getMeasurements();
            LOG.debug("Call: {} VM: {} vmMeasurements: {} measurements: {}", peassNode.getCall(), 
                  vm, vmMeasurements.size(), measurements.size());
            if(measurements.size() > 1) {
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

    public static CallTreeNode createOtherNodeRecursive(final StackTraceTreeNode otherNode, StackTraceTreeNode node,
                                                        CallTreeNode otherCallTreeNode, String predecessor,
                                                        String commit, MeasurementConfig config) {
        if (commit == null && predecessor == null) {
            throw new IllegalArgumentException("Commit and Predecessor cannot be null");
        }

        node = node != null ? search(otherNode, node) : null;

        final String methodNameWithNew = normalizeKiekerPattern(otherNode);
        final String call = KiekerPatternConverter.getCall(methodNameWithNew);
        if (otherCallTreeNode == null) {
            otherCallTreeNode = new CallTreeNode(call,
                    methodNameWithNew,
                    methodNameWithNew,
                    config);
//            createPeassNode(otherNode, node, otherCallTreeNode, commit, predecessor, true, config);
        } else {
//            createPeassNode(otherNode, node, otherCallTreeNode, commit, predecessor, false, config);
            otherCallTreeNode = otherCallTreeNode.getChildByKiekerPattern(methodNameWithNew);
        }
        
        if (otherCallTreeNode != null) {
           List<StackTraceTreeNode> children = otherNode.getChildren();
//           if (children.isEmpty() && otherCallTreeNode != null) {
//               createPeassNode(otherNode, node, otherCallTreeNode, commit, predecessor, true, config);
//           }
           for (StackTraceTreeNode child : children) {
               createOtherNodeRecursive(child, node, otherCallTreeNode , predecessor, commit, config);
           }
        } else {
           LOG.warn("Didn't find other call tree node for " + methodNameWithNew + " (Call: " + call + ")");
        }

        

        return otherCallTreeNode;
    }
}
