package de.dagere.peass.measurement.rca;

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
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.kieker.KiekerPatternConverter;
import de.dagere.peass.measurement.rca.kiekerReading.KiekerDurationReader;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.traces.KiekerFolderUtil;
import kieker.analysis.exception.AnalysisConfigurationException;

public class KiekerResultReader {

   private static final Logger LOG = LogManager.getLogger(KiekerResultReader.class);

   final boolean useAggregation;
   final Set<CallTreeNode> includedNodes;
   final String version;
   final TestCase testcase;
   final boolean otherVersion;

   boolean considerNodePosition = false;

   public KiekerResultReader(final boolean useAggregation, final Set<CallTreeNode> includedNodes, final String version,
         final TestCase testcase, final boolean otherVersion) {
      this.useAggregation = useAggregation;
      this.includedNodes = includedNodes;
      this.version = version;
      this.testcase = testcase;
      this.otherVersion = otherVersion;
   }

   public void setConsiderNodePosition(final boolean considerNodePosition) {
      this.considerNodePosition = considerNodePosition;
   }

   public void readResults(final File versionResultFolder) {
      try {
         LOG.info("Reading kieker results from {}", versionResultFolder.getAbsolutePath(), version);
         FileFilter filter = new OrFileFilter(new RegexFileFilter("[0-9]*"), new RegexFileFilter("measurement-[0-9]*.csv"));
         final File[] kiekerResultFiles = versionResultFolder.listFiles(filter);
         for (final File kiekerResultFolder : kiekerResultFiles) {
            final File kiekerTraceFile = KiekerFolderUtil.getKiekerTraceFolder(kiekerResultFolder, testcase);
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
      for (final CallTreeNode node : includedNodes) {
         readNode(fullDataMap, node);
      }
   }

   private void readNode(final Map<AggregatedDataNode, AggregatedData> fullDataMap, final CallTreeNode node) {
      boolean nodeFound = false;
      final CallTreeNode examinedNode = otherVersion ? node.getOtherVersionNode() : node;
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
         LOG.debug("Setting measurement: {} {} {}", version, nodeCall, values.size());
         // System.out.println(StatisticUtil.getMean(values) + " ");
         node.addAggregatedMeasurement(version, values);
      } else {
         LOG.warn("Node {} ({}) did not find measurement values, measured methods: {}", nodeCall, node.getOtherVersionNode(), fullDataMap.entrySet().size());
      }
   }

   private boolean isSameNode(final CallTreeNode node, final String nodeCall, final AggregatedDataNode measuredNode) {
      final String kiekerCall = KiekerPatternConverter.getKiekerPattern(measuredNode.getCall());
      LOG.trace("Node: {} Kieker: {} Equal: {}", nodeCall, kiekerCall, nodeCall.equals(kiekerCall));
      if (nodeCall.equals(kiekerCall)) {
         LOG.trace("EOI: {} vs {}", node.getEoi(), measuredNode.getEoi());
         if (considerNodePosition) {
            final int eoi = node.getEoi();
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
      KiekerDurationReader.executeDurationStage(kiekerTraceFolder, includedNodes, version);
   }
}
