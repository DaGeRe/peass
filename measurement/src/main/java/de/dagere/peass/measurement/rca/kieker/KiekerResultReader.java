package de.dagere.peass.measurement.rca.kieker;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.kopeme.kieker.aggregateddata.AggregatedData;
import de.dagere.kopeme.kieker.aggregateddata.AggregatedDataNode;
import de.dagere.kopeme.kieker.writer.AggregatedDataReader;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependency.traces.KiekerFolderUtil;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kiekerReading.KiekerDurationReader;
import kieker.analysis.exception.AnalysisConfigurationException;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;

public class KiekerResultReader {

   private static final Logger LOG = LogManager.getLogger(KiekerResultReader.class);

   final boolean useAggregation;
   private final AllowedKiekerRecord usedRecord;
   private final Set<CallTreeNode> includedNodes;
   final String commit;
   final TestCase testcase;
   final boolean otherCommit;

   boolean considerNodePosition = false;

   public KiekerResultReader(final boolean useAggregation, final AllowedKiekerRecord usedRecord, final Set<CallTreeNode> includedNodes, final String commit,
         final TestCase testcase, final boolean otherCommit) {
      this.useAggregation = useAggregation;
      this.usedRecord = usedRecord;
      this.includedNodes = includedNodes;
      this.commit = commit;
      this.testcase = testcase;
      this.otherCommit = otherCommit;
   }

   public void setConsiderNodePosition(final boolean considerNodePosition) {
      this.considerNodePosition = considerNodePosition;
   }

   public void readResults(final File versionResultFolder) {
      try {
         LOG.info("Reading kieker results from {}", versionResultFolder.getAbsolutePath(), commit);
         FileFilter filter = new OrFileFilter(new RegexFileFilter("[0-9]*"), new RegexFileFilter("measurement-[0-9]*.csv"));
         final File[] kiekerResultFiles = versionResultFolder.listFiles(filter);
         for (final File kiekerResultFolder : kiekerResultFiles) {
            final File kiekerTraceFile = KiekerFolderUtil.getKiekerTraceFolder(kiekerResultFolder, testcase);
            LOG.debug("Reading file: {}", kiekerTraceFile.getAbsolutePath());
            if (useAggregation) {
               readAggregatedData(kiekerTraceFile);
            } else {
               readNonAggregated(kiekerTraceFile);
            }
         }
      } catch (IllegalStateException | AnalysisConfigurationException | IOException e) {
         e.printStackTrace();
      }
   }

   public void readAggregatedData(final File kiekerTraceFolder) throws JsonParseException, JsonMappingException, IOException {
      final Map<AggregatedDataNode, AggregatedData> fullDataMap = AggregatedDataReader.getFullDataMap(kiekerTraceFolder);
      if (fullDataMap.isEmpty()) {
         LOG.warn("No data were measured - a measurement error occured");
      } else {
         for (final CallTreeNode node : includedNodes) {
            readNode(fullDataMap, node);
         }
      }
   }

   private void readNode(final Map<AggregatedDataNode, AggregatedData> fullDataMap, final CallTreeNode node) {
      boolean nodeFound = false;
      final CallTreeNode examinedNode = otherCommit ? node.getOtherVersionNode() : node;
      final String nodeCall = KiekerPatternConverter.fixParameters(examinedNode.getKiekerPattern());
      final List<StatisticalSummary> values = new LinkedList<>();
      for (final Entry<AggregatedDataNode, AggregatedData> entry : fullDataMap.entrySet()) {
         if (isSameNode(examinedNode, nodeCall, entry.getKey())) {
            for (final StatisticalSummary dataSlice : entry.getValue().getStatistic().values()) {
               values.add(dataSlice);
            }
            nodeFound = true;
         }
      }

      if (nodeFound) {
         LOG.debug("Setting measurement: {} {} {}", commit, nodeCall, values.size());
         // System.out.println(StatisticUtil.getMean(values) + " ");
         node.addAggregatedMeasurement(commit, values);
      } else {
         LOG.warn("Node {} ({}) did not find measurement values, measured methods: {}", nodeCall, node.getOtherKiekerPattern(), fullDataMap.entrySet().size());
      }
   }

   private boolean isSameNode(final CallTreeNode node, final String nodeCall, final AggregatedDataNode measuredNode) {
      final String kiekerCall = KiekerPatternConverter.getKiekerPattern(measuredNode.getCall());
      LOG.trace("Node: {} Kieker: {} Equal: {}", nodeCall, kiekerCall, nodeCall.equals(kiekerCall));
      if (nodeCall.equals(kiekerCall) || isSameNodeWithoutModifier(nodeCall, kiekerCall)) {
         LOG.trace("EOI: {} vs {}", node.getEoi(commit), measuredNode.getEoi());
         if (considerNodePosition) {
            final int eoi = node.getEoi(commit);
            if (measuredNode.getEoi() == eoi) {
               return true;
            } else {
               return false;
            }
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   public void readNonAggregated(final File kiekerTraceFolder) throws AnalysisConfigurationException {
      if (usedRecord == AllowedKiekerRecord.OPERATIONEXECUTION) {
         KiekerDurationReader.executeDurationStage(kiekerTraceFolder, includedNodes, commit);
      } else if (usedRecord == AllowedKiekerRecord.DURATION) {
         KiekerDurationReader.executeReducedDurationStage(kiekerTraceFolder, includedNodes, commit);
      }
   }

   private boolean isSameNodeWithoutModifier(String nodeCall, String kiekerCall) {
      String executable = testcase.getExecutable().replace("#", ".");
      String pureCall = nodeCall.substring("public ".length());
      return nodeCall.contains(executable) && pureCall.equals(kiekerCall);
   }
}
