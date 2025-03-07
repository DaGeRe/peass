package de.dagere.peass.measurement.utils.sjsw;

import com.google.common.collect.Lists;
import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
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

    public static CallTreeNode convertCallContextTreeToCallTree(StackTraceTreeNode currentBAT,
                                                                StackTraceTreeNode predecessorBAT, CallTreeNode ctn,
                                                                String commit, String predecessor, MeasurementConfig config) {
        if (commit == null && predecessor == null) {
            throw new IllegalArgumentException("Commit and Predesseror cannot be null");
        }

        LOG.info("Current original node: {}", currentBAT.getPayload().getMethodName());

        StackTraceTreeNode otherNode = predecessorBAT != null ? search(currentBAT, predecessorBAT) : null;

        LOG.info("Other original node: {}", otherNode != null ? otherNode.getPayload().getMethodName() : null);
        LOG.info("Original node: {}", predecessorBAT != null ? predecessorBAT.getPayload().getMethodName() : null);

        String methodNameWithNew = normalizeKiekerPattern(currentBAT);
        if(methodNameWithNew.contains("<init>")) {
            methodNameWithNew = "new " + methodNameWithNew;
        }
        String call = getCall(methodNameWithNew);
        if(ctn == null) {
            ctn = new CallTreeNode(call,
                    methodNameWithNew,
                    methodNameWithNew,
                    config);
            createPeassNode(currentBAT, otherNode, ctn, commit, predecessor, true, config);
        } else {
            createPeassNode(currentBAT, otherNode, ctn, commit, predecessor, false, config);
            ctn = ctn.getChildByKiekerPattern(methodNameWithNew);
        }

        if (otherNode != null) {
            CallTreeNode otherCallTreeNode = null;
            otherCallTreeNode = createOtherNodeRecursive(otherNode, currentBAT, otherCallTreeNode, commit, predecessor, config);
            ctn.setOtherCommitNode(otherCallTreeNode);
        }

        List<StackTraceTreeNode> children = currentBAT.getChildren();
        if (children.isEmpty() && ctn != null) {
            createPeassNode(currentBAT, otherNode, ctn, commit, predecessor, true, config);
        }
        for (StackTraceTreeNode child : children) {
            if (child != null) {
                convertCallContextTreeToCallTree(child, otherNode, ctn, commit, predecessor, config);
            }
        }

        return ctn;
    }

   private static String getCall(String methodNameWithNew) {
        int indexOfParenthesis = methodNameWithNew.indexOf('(');
        
        String partBeforeParenthesis = methodNameWithNew.substring(0, indexOfParenthesis);
        String parameters = methodNameWithNew.substring(indexOfParenthesis).replace(" ", "");
        int methodSeperatorIndex = partBeforeParenthesis.lastIndexOf('.');
        String clazz = partBeforeParenthesis.substring(0, methodSeperatorIndex);
        String method = partBeforeParenthesis.substring(methodSeperatorIndex + 1) ;
        
        String call = clazz + MethodCall.METHOD_SEPARATOR + method + parameters; 
      return call;
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

    private static void createPeassNode(StackTraceTreeNode node, StackTraceTreeNode otherNode, CallTreeNode peassNode,
                                        String commit, String oldCommit, boolean lastNode,
                                        MeasurementConfig config) {
        LOG.info("Creating peass node for stacktracetreenodes: {} -> {}", node.getPayload().getMethodName() + "(" + node.getMeasurements() + ")", otherNode != null ? otherNode.getPayload().getMethodName() + "(" + otherNode.getMeasurements() + ")" : null);
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
            peassNode.createStatistics(commit);
            peassNode.createStatistics(oldCommit);
        }

        if(!lastNode) appendChild(node, peassNode);

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
        String call = getCall(methodNameWithNew);
        if(node.getPayload().getMethodName().contains("<init>")) {
            methodNameWithNew = "new " + methodNameWithNew;
            peassNode.appendChild(call,
                    methodNameWithNew,
                    methodNameWithNew
            );
        } else {
            peassNode.appendChild(call,
                    methodNameWithNew,
                    methodNameWithNew
            );
        }
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

    private static void addIterativeMeasurements(String commit, StackTraceTreeNode node, CallTreeNode peassNode,
                                                 int vms, int iterations) {
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
            List<Double> measurements = vmMeasurements.get(0).getMeasurements();
            final List<StatisticalSummary> values = new LinkedList<>();
            if(measurements.size() > 1) {
                int sliceSize = Math.max(measurements.size() / iterations, 1);
                LOG.info("Measurements: {} Iterations: {} Slice size: {}", measurements.size(), iterations, sliceSize);
                List<List<Double>> slicedIterationMeasurements = Lists.partition(measurements, sliceSize);
                slicedIterationMeasurements.forEach(slice -> {
                    final SummaryStatistics statistic = new SummaryStatistics();
                    slice.forEach(measurement -> {
                        statistic.addValue((long) (double) measurement);
                        System.out.println("Adding sjsw measurement: " + measurement);
                    });
                    values.add(statistic);
                });
                peassNode.addAggregatedMeasurement(commit, values);
            } else {
                peassNode.initVMData(commit);
                measurements.forEach(measurement -> {
                    peassNode.addMeasurement(commit, (long) (double) measurement);
                });
            }
        }
    }

    public static CallTreeNode createOtherNodeRecursive(StackTraceTreeNode otherNode, StackTraceTreeNode node,
                                                        CallTreeNode otherCallTreeNode, String predecessor,
                                                        String commit, MeasurementConfig config) {
        if (commit == null && predecessor == null) {
            throw new IllegalArgumentException("Commit and Predecessor cannot be null");
        }

        node = node != null ? search(otherNode, node) : null;

        String methodNameWithNew = normalizeKiekerPattern(otherNode);
        if(otherNode.getPayload().getMethodName().contains("<init>")) {
            methodNameWithNew = "new " + otherNode.getPayload().getMethodName();
        }
        String call = getCall(methodNameWithNew);
        if(otherCallTreeNode == null) {
            otherCallTreeNode = new CallTreeNode(call,
                    methodNameWithNew,
                    methodNameWithNew,
                    config);
            createPeassNode(otherNode, node, otherCallTreeNode, commit, predecessor, true, config);
        } else {
            createPeassNode(otherNode, node, otherCallTreeNode, commit, predecessor, false, config);
            otherCallTreeNode = otherCallTreeNode.getChildByKiekerPattern(methodNameWithNew);
        }
        
        if (otherCallTreeNode != null) {
           List<StackTraceTreeNode> children = otherNode.getChildren();
           if (children.isEmpty() && otherCallTreeNode != null) {
               createPeassNode(otherNode, node, otherCallTreeNode, commit, predecessor, true, config);
           }
           for (StackTraceTreeNode child : children) {
               createOtherNodeRecursive(child, node, otherCallTreeNode , predecessor, commit, config);
           }
        } else {
           LOG.warn("Didn't find other call tree node for " + methodNameWithNew + " (Call: " + call + ")");
        }

        

        return otherCallTreeNode;
    }
}
