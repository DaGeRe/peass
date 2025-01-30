package de.dagere.peass.measurement.utils;

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.analyzer.CompleteTreeAnalyzer;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.utils.sjsw.SjswCctConverter;
import io.github.terahidro2003.result.tree.StackTraceTreeBuilder;
import io.github.terahidro2003.result.tree.StackTraceTreeNode;
import io.github.terahidro2003.samplers.jfr.ExecutionSample;
import io.github.terahidro2003.samplers.jfr.Method;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.*;

public class SjswCctConverterTest {
    private List<String> path1a = new ArrayList<>(List.of("org.example.testing", "org.example.methodA", "org.example.methodB"));
    private List<String> path1b = new ArrayList<>(List.of("org.example.testing", "org.example.methodA", "org.example.someOtherMethod", "libjvm.so.Runtime1::counter_overflow()"));
    private List<String> path2 = new ArrayList<>(List.of("org.example.testing", "org.example.methodB"));

    @BeforeEach
    void prepare() {
        reverseStacktraces();
    }

    @Test
    public void testRecursiveOtherTreeCreation() {
        int vms = 2;
        String commit = "a1";
        String oldCommit = "b2";
        StackTraceTreeNode current = prepareFakeTree(List.of(path1a, path1b), commit, vms);
        current.printTree();
        CallTreeNode root = null;
        root = SjswCctConverter.createOtherNodeRecursive(current, root, vms, commit, oldCommit);
        printCallTreeNode(root);
    }

    @Test
    public void testWithoutEmptyNodes() {
        int vms = 2;
        String commit = "a1";
        String oldCommit = "b2";
        StackTraceTreeNode current = prepareFakeTree(List.of(path1a, path1b), commit, vms);
        StackTraceTreeNode old = prepareFakeTree(List.of(path1a, path2),
                oldCommit, vms);

        printTrees(current, old);

        CallTreeNode root = null;
        root = SjswCctConverter.convertCallContextTreeToCallTree(current, old, root, commit, oldCommit, vms);

        CompleteTreeAnalyzer analyzer = new CompleteTreeAnalyzer(root, root.getOtherCommitNode());
        var bla = root.getOtherCommitNode();
        if(bla == null) {
            throw new RuntimeException("Other commit node is null");
        }

        printCallTreeNode(root);
        System.out.println();
        printCallTreeNode(root.getOtherCommitNode());

        reproduceToEntityProblem(root, root.getOtherCommitNode());
    }

    @Test
    public void testCorrectCallString() {
        String methodSignature = "public void org.example.testing(int, long)";
        String call = SjswCctConverter.getCorrectCallString(methodSignature);
        Assert.assertEquals("org.example#testing", call);
    }

    @Test
    public void testCTNToEntity() {
        String methodSignature = "public void org.example.testing(int, long)";
        String call = SjswCctConverter.getCorrectCallString(methodSignature);
        final CallTreeNode node = new CallTreeNode(call, methodSignature, methodSignature, (MeasurementConfig) null);
        System.out.println("Node created: " + node.getCall() + ", " + node.getMethod());
        MethodCall result = node.toEntity();
        Assert.assertEquals(new MethodCall("org.example", "", "testing"), node.toEntity());
    }

    @Test
    public void testCTNToEntityWithRootNode() {
        String methodSignature = "Root.root()";
        String call = "Root#root";
//        String call = SjswCctConverter.getCorrectCallString(methodSignature);
        final CallTreeNode node = new CallTreeNode(call, methodSignature, methodSignature, (MeasurementConfig) null);
        System.out.println("Node created: " + node.getCall() + ", " + node.getMethod());
        MethodCall result = node.toEntity();
        Assert.assertEquals(new MethodCall("Root", "", "root"), node.toEntity());
    }

    private void reverseStacktraces() {
        Collections.reverse(path1a);
        Collections.reverse(path1b);
        Collections.reverse(path2);
    }

    private void printTrees(StackTraceTreeNode current, StackTraceTreeNode old) {
        current.printTree();
        System.out.println();
        System.out.println();
        old.printTree();
    }

    public static void printCallTreeNode(CallTreeNode root) {
        printCallTreeNodeTreeRecursive(root, "", false);
    }

    public static void printCallTreeNodeTreeRecursive(CallTreeNode node, String prefix, boolean isLast) {
        if (node.getCall() != null) {
            System.out.println(prefix + (isLast ? "└────── " : "├────── ") + node.getCall() +
                    " Keys: [" + node.getKeys() + "]");
        }

        List<CallTreeNode> children = node.getChildren();
        for (int i = 0; i < children.size(); i++) {
            printCallTreeNodeTreeRecursive(children.get(i), prefix + (isLast ? "    " : "│   "), i == children.size() - 1);
        }
    }

    private StackTraceTreeNode prepareFakeTree(List<List<String>> pathsAsMethodNames, String commit, int vms) {
        List<ExecutionSample> samples = new ArrayList<>();
        pathsAsMethodNames.forEach(path -> {
            samples.add(getMockExecutionSample(path));
        });

        StackTraceTreeBuilder treeBuilder = new StackTraceTreeBuilder();
        StackTraceTreeNode tree = treeBuilder.buildFromExecutionSamples(samples);
        addFakeMeasurements(tree, commit, vms);

        return tree;
    }

    private void addFakeMeasurements(StackTraceTreeNode root, String commit, int vms) {
        Stack<StackTraceTreeNode> stack = new Stack<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            StackTraceTreeNode currentNode = stack.pop();

            int randomAmountofSamples = RandomUtils.nextInt(1, 1000);
            for (int i = 0; i < vms; i++) {
                int amount = randomAmountofSamples * RandomUtils.nextInt(1, 5);
                currentNode.addMeasurement(commit, (double) amount);
            }

            for (StackTraceTreeNode child : currentNode.getChildren()) {
                if (child != null) {
                    stack.push(child);
                }
            }
        }
    }

    private ExecutionSample getMockExecutionSample(List<String> stacktraceAsString) {
        List<Method> stacktrace = new ArrayList<>();
        stacktraceAsString.forEach(s -> {
            Method method = new Method();
            method.setMethodName(s);
            method.setMethodModifier("public void");
            method.setMethodDescriptor("int, long");
            stacktrace.add(method);
        });
        ExecutionSample sample = new ExecutionSample();
        sample.setStackTrace(stacktrace);
        return sample;
    }

    private void reproduceToEntityProblem(CallTreeNode root, CallTreeNode rootPredecessor) {
       root.toEntity();
       List<CallTreeNode> children = root.getChildren();
       for (CallTreeNode child : children) {
          reproduceToEntityProblem(child, child.getOtherCommitNode());
       }
    }
}
