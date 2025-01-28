package de.dagere.peass.measurement.utils.sjsw;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import io.github.terahidro2003.result.tree.StackTraceTreeNode;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

            createPeassNode(currentBAT, ctn, commit, predecessor, vms);
        } else {
            createPeassNode(currentBAT, ctn, commit, predecessor, vms);
            ctn = ctn.getChildByKiekerPattern(currentBAT.getPayload().getMethodName() + "()");
        }

        List<StackTraceTreeNode> children = currentBAT.getChildren();
        for (StackTraceTreeNode child : children) {
            convertCallContextTreeToCallTree(child, null, ctn, commit, predecessor, vms);
        }

        return ctn;
    }

    private static void createPeassNode(StackTraceTreeNode node, CallTreeNode peassNode, String commit, String oldCommit, int vms) {
        peassNode.initCommitData();

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
        SummaryStatistics statistics = peassNode.getStatistics(commit);
        if(statistics == null) {
            log.info("Statistics is null. Attempting to provide empty statistics.");
            peassNode.addMeasurement(commit, 0L);
            statistics = peassNode.getStatistics(commit);
            if(statistics == null) {
                log.info("Unable to provide empty statistics");
            }
        }
    }
}
