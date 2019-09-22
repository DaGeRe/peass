package de.peass.measurement.searchcause;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.peass.dependency.analysis.KiekerReader;
import de.peass.dependency.analysis.PeASSFilter;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.dependency.traces.KiekerFolderUtil;
import de.peass.measurement.searchcause.data.CallTreeNode;
import de.peass.measurement.searchcause.kieker.DurationFilter;
import de.peass.measurement.searchcause.kieker.KiekerPatternConverter;
import kieker.analysis.AnalysisController;
import kieker.analysis.exception.AnalysisConfigurationException;
import kieker.monitoring.writer.filesystem.AggregatedDataReader;
import kieker.monitoring.writer.filesystem.aggregateddata.AggregatedData;
import kieker.tools.traceAnalysis.filter.executionRecordTransformation.ExecutionRecordTransformationFilter;

public class KiekerResultReader {
   
   private static final Logger LOG = LogManager.getLogger(KiekerResultReader.class);
   
   final boolean useAggregation;
   final Set<CallTreeNode> includedNodes;
   final String version;
   final File versionResultFolder;
   final TestCase testcase;
   final boolean otherVersion;

   public KiekerResultReader(final boolean useAggregation, final Set<CallTreeNode> includedNodes, final String version,
         final File versionResultFolder, final TestCase testcase, final boolean otherVersion) {
      this.useAggregation = useAggregation;
      this.includedNodes = includedNodes;
      this.version = version;
      this.versionResultFolder = versionResultFolder;
      this.testcase = testcase;
      this.otherVersion = otherVersion;
   }

   public void readResults() {
      try {
         for (final File kiekerResultFolder : versionResultFolder.listFiles((FilenameFilter) new RegexFileFilter("[0-9]*"))) {
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

   private void readAggregatedData(final File kiekerTraceFile) throws JsonParseException, JsonMappingException, IOException {
      final Map<kieker.monitoring.writer.filesystem.aggregateddata.AggregatedDataNode, AggregatedData> fullDataMap = AggregatedDataReader.getFullDataMap(kiekerTraceFile);
      for (final CallTreeNode node : includedNodes) {
         boolean nodeFound = false;
         final String nodeCall = KiekerPatternConverter.fixParameters(otherVersion ? node.getOtherVersionNode().getKiekerPattern() : node.getKiekerPattern());
         for (final Entry<kieker.monitoring.writer.filesystem.aggregateddata.AggregatedDataNode, AggregatedData> entry : fullDataMap.entrySet()) {
            final String kiekerCall = KiekerPatternConverter.getKiekerPattern(entry.getKey().getCall());
            if (nodeCall.equals(kiekerCall)) {
               node.setMeasurement(version, entry.getValue().getStatistic());
               nodeFound = true;
            }
         }
         if (!nodeFound) {
            LOG.warn("Node {} ({}) did not find measurement values", nodeCall);
         }
      }
   }

   public void readNonAggregated(final File kiekerTraceFile) throws AnalysisConfigurationException {
      final KiekerReader reader = new KiekerReader(kiekerTraceFile);
      reader.initBasic();
      final AnalysisController analysisController = reader.getAnalysisController();

      final DurationFilter kopemeFilter = new DurationFilter(includedNodes, analysisController, version);
      analysisController.connect(reader.getExecutionFilter(), ExecutionRecordTransformationFilter.OUTPUT_PORT_NAME_EXECUTIONS,
            kopemeFilter, PeASSFilter.INPUT_EXECUTION_TRACE);

      analysisController.run();
   }
}
