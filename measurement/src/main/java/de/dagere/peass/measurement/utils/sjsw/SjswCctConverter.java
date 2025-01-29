package de.dagere.peass.measurement.utils.sjsw;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import io.github.terahidro2003.result.tree.StackTraceTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Stack;

public class SjswCctConverter {
    private static final Logger log = LoggerFactory.getLogger(SjswCctConverter.class);

    public static CallTreeNode convertCallContextTreeToCallTree(StackTraceTreeNode currentBAT, StackTraceTreeNode predecessorBAT, CallTreeNode ctn, String commit, String predecessor, int vms) {
        if (commit == null && predecessor == null) {
            throw new IllegalArgumentException("Commit and Predesseror cannot be null");
        }

        MeasurementConfig mConfig = new MeasurementConfig(vms, commit, predecessor);

        if(ctn == null) {
            String methodNameWithNew = currentBAT.getPayload().getMethodName() + "()";
            if(currentBAT.getPayload().getMethodName().contains("<init>")) {
                methodNameWithNew = "new " + currentBAT.getPayload().getMethodName() + "()";
            }
            ctn = new CallTreeNode(currentBAT.getPayload().getMethodName(),
                    methodNameWithNew,
                    methodNameWithNew,
                    mConfig);
        } else {
            createPeassNode(currentBAT, ctn, commit, predecessor, vms);
            ctn = ctn.getChildByKiekerPattern(currentBAT.getPayload().getMethodName() + "()");
        }

        StackTraceTreeNode otherNode = predecessorBAT != null ? search(predecessorBAT, currentBAT) : null;
        if (otherNode != null) {
            CallTreeNode otherCallTreeNode = null;
            otherCallTreeNode = createOtherNodeRecursive(otherNode, otherCallTreeNode, vms, predecessor, commit);
            ctn.setOtherCommitNode(otherCallTreeNode);
        }

        List<StackTraceTreeNode> children = currentBAT.getChildren();
        for (StackTraceTreeNode child : children) {
            convertCallContextTreeToCallTree(child, predecessorBAT, ctn, commit, predecessor, vms);
        }

        return ctn;
    }

    public static void printCallTreeNode(CallTreeNode root) {
        printCallTreeNodeTreeRecursive(root, "", false);
    }

    public static void printCallTreeNodeTreeRecursive(CallTreeNode node, String prefix, boolean isLast) {
        if (node.getMethod() != null) {
            System.out.println(prefix + (isLast ? "└────── " : "├────── ") + node.getMethod() +
                    " [Measurements: NA");
        }

        List<CallTreeNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            printCallTreeNodeTreeRecursive(children.get(i), prefix + (isLast ? "    " : "│   "), i == children.size() - 1);
        }
    }

    public static StackTraceTreeNode search(StackTraceTreeNode searchable, StackTraceTreeNode tree) {
        Stack<StackTraceTreeNode> stack = new Stack<>();
        stack.push(tree);

        while (!stack.isEmpty()) {
            StackTraceTreeNode currentNode = stack.pop();

            if(searchable.equals(currentNode)) {
                return searchable;
            }

            for (StackTraceTreeNode child : currentNode.getChildren()) {
                if (child != null) {
                    stack.push(child);
                }
            }
        }

        return null;
    }

    private static void createPeassNode(StackTraceTreeNode node, CallTreeNode peassNode, String commit, String oldCommit, int vms) {
        peassNode.initCommitData();

        addMeasurements(commit, node, peassNode, vms);

        // check is done as a workaround for Peass kieker pattern check
        if(node.getPayload().getMethodName().contains("<init>")) {
            String methodNameWithNew = "new " + node.getPayload().getMethodName() + "()";
            peassNode.appendChild(node.getPayload().getMethodName(),
                    methodNameWithNew,
                    methodNameWithNew
            );
        } else {
            peassNode.appendChild(node.getPayload().getMethodName(),
                    node.getPayload().getMethodName() + "()",
                    node.getPayload().getMethodName() + "()"
            );
        }

        peassNode.createStatistics(commit);
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

    public static CallTreeNode createOtherNodeRecursive(StackTraceTreeNode otherNode, CallTreeNode otherCallTreeNode, int vms, String predecessor, String commit) {
        if (commit == null && predecessor == null) {
            throw new IllegalArgumentException("Commit and Predesseror cannot be null");
        }

        MeasurementConfig mConfig = new MeasurementConfig(vms, predecessor, commit);

        if(otherCallTreeNode == null) {
            String methodNameWithNew = otherNode.getPayload().getMethodName() + "()";
            if(otherNode.getPayload().getMethodName().contains("<init>")) {
                methodNameWithNew = "new " + otherNode.getPayload().getMethodName() + "()";
            }
            otherCallTreeNode = new CallTreeNode(otherNode.getPayload().getMethodName(),
                    methodNameWithNew,
                    methodNameWithNew,
                    mConfig);
        } else {
            createPeassNode(otherNode, otherCallTreeNode, predecessor, commit, vms);
            otherCallTreeNode = otherCallTreeNode.getChildByKiekerPattern(otherNode.getPayload().getMethodName() + "()");
//            otherCallTreeNode.createStatistics(predecessor);
        }

        List<StackTraceTreeNode> children = otherNode.getChildren();
        for (StackTraceTreeNode child : children) {
            createOtherNodeRecursive(child, otherCallTreeNode , vms, predecessor, commit);
        }

        return otherCallTreeNode;
    }
}
