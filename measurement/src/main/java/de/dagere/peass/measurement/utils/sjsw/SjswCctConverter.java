package de.dagere.peass.measurement.utils.sjsw;

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import io.github.terahidro2003.result.tree.StackTraceTreeNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Stack;

public class SjswCctConverter {
    private static final Logger LOG = LoggerFactory.getLogger(SjswCctConverter.class);

    public static CallTreeNode convertCallContextTreeToCallTree(StackTraceTreeNode currentBAT, StackTraceTreeNode predecessorBAT, CallTreeNode ctn, String commit, String predecessor, int vms) {
        if (commit == null && predecessor == null) {
            throw new IllegalArgumentException("Commit and Predesseror cannot be null");
        }

        MeasurementConfig mConfig = new MeasurementConfig(vms, commit, predecessor);

        String methodNameWithNew = normalizeKiekerPattern(currentBAT);
        String call = getCall(methodNameWithNew);
        if(ctn == null) {
            if(methodNameWithNew.contains("<init>")) {
                methodNameWithNew = "new " + methodNameWithNew;
            }
            ctn = new CallTreeNode(call,
                    methodNameWithNew,
                    methodNameWithNew,
                    mConfig);
        } else {
            createPeassNode(currentBAT, ctn, commit, vms, false);
            ctn = ctn.getChildByKiekerPattern(methodNameWithNew);
        }

        StackTraceTreeNode otherNode = predecessorBAT != null ? search(predecessorBAT, currentBAT) : null;
        if (otherNode != null) {
            CallTreeNode otherCallTreeNode = null;
            otherCallTreeNode = createOtherNodeRecursive(otherNode, otherCallTreeNode, vms, predecessor, commit);
            ctn.setOtherCommitNode(otherCallTreeNode);
        }

        List<StackTraceTreeNode> children = currentBAT.getChildren();
        if (children.isEmpty()) {
            createPeassNode(currentBAT, ctn, commit, vms, true);
        }
        for (StackTraceTreeNode child : children) {
            convertCallContextTreeToCallTree(child, predecessorBAT, ctn, commit, predecessor, vms);
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

    private static void createPeassNode(StackTraceTreeNode node, CallTreeNode peassNode, String commit,
                                        int vms, boolean lastNode) {
        peassNode.initCommitData();

        addMeasurements(commit, node, peassNode, vms);

        if(!lastNode) appendChild(node, peassNode);

        peassNode.createStatistics(commit);
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

    public static CallTreeNode createOtherNodeRecursive(StackTraceTreeNode otherNode, CallTreeNode otherCallTreeNode, int vms, String predecessor, String commit) {
        if (commit == null && predecessor == null) {
            throw new IllegalArgumentException("Commit and Predesseror cannot be null");
        }

        MeasurementConfig mConfig = new MeasurementConfig(vms, predecessor, commit);

        String methodNameWithNew = normalizeKiekerPattern(otherNode);
        String call = getCall(methodNameWithNew);
        if(otherCallTreeNode == null) {
            if(otherNode.getPayload().getMethodName().contains("<init>")) {
                methodNameWithNew = "new " + otherNode.getPayload().getMethodName();
            }
            otherCallTreeNode = new CallTreeNode(call,
                    methodNameWithNew,
                    methodNameWithNew,
                    mConfig);
        } else {
            createPeassNode(otherNode, otherCallTreeNode, predecessor, vms, false);
            otherCallTreeNode = otherCallTreeNode.getChildByKiekerPattern(methodNameWithNew);
        }
        
        if (otherCallTreeNode != null) {
           List<StackTraceTreeNode> children = otherNode.getChildren();
           if (children.isEmpty()) {
               createPeassNode(otherNode, otherCallTreeNode, predecessor, vms, true);
           }
           for (StackTraceTreeNode child : children) {
               createOtherNodeRecursive(child, otherCallTreeNode , vms, predecessor, commit);
           }
        } else {
           LOG.warn("Didn't find other call tree node for " + methodNameWithNew + " (Call: " + call + ")");
        }

        

        return otherCallTreeNode;
    }
}
