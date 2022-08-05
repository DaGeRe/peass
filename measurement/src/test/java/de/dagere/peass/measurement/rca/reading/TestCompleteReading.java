package de.dagere.peass.measurement.rca.reading;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.dagere.kopeme.kieker.writer.AggregatedTreeWriter;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.testData.TestMethodCall;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.kieker.KiekerResultReader;
import kieker.analysis.exception.AnalysisConfigurationException;
import kieker.common.configuration.Configuration;
import kieker.common.record.controlflow.OperationExecutionRecord;
import net.kieker.sourceinstrumentation.AllowedKiekerRecord;

public class TestCompleteReading {

   private final CallTreeNode root = new CallTreeNode("parent()", "public void A.parent()", null, new MeasurementConfig(3, "0", "1"));

   @Test
   public void testReading() throws AnalysisConfigurationException, JsonParseException, JsonMappingException, IOException {
      final File kiekerTraceFolder = getKiekerTraceFolder();

      writeFakeMeasurements(kiekerTraceFolder, true);

      final Set<CallTreeNode> includedNodes = buildTree();

      executeReading(kiekerTraceFolder, includedNodes);

      checkResults(includedNodes);
   }

   @Test
   public void testReadingNoModifier() throws AnalysisConfigurationException, JsonParseException, JsonMappingException, IOException {
      final File kiekerTraceFolder = getKiekerTraceFolder();

      writeFakeMeasurements(kiekerTraceFolder, false);

      final Set<CallTreeNode> includedNodes = buildTree();

      executeReading(kiekerTraceFolder, includedNodes);

      checkResults(includedNodes);
   }
   
   private void checkResults(final Set<CallTreeNode> includedNodes) {
      for (final CallTreeNode node : includedNodes) {
         node.createStatistics("0");
         if (!node.getCall().equals(CauseSearchData.ADDED)) {
            Assert.assertEquals(node.getCall() + " not found", 1, node.getStatistics("0").getN());
         }
      }

      for (final CallTreeNode node : root.getChildren()) {
         if (!node.getCall().equals(CauseSearchData.ADDED)) {
            Assert.assertEquals(node.getCall() + " has wrong executions", 100, node.getCallCount("0"));
         }
      }
   }

   private void executeReading(final File kiekerTraceFolder, final Set<CallTreeNode> includedNodes) throws JsonParseException, JsonMappingException, IOException {
      final TestMethodCall testcase = new TestMethodCall("A", "parent");
      final KiekerResultReader reader = new KiekerResultReader(false, AllowedKiekerRecord.OPERATIONEXECUTION, includedNodes, "0", testcase, false);
      // reader.setConsiderNodePosition(true);
      final File kiekerFolder = kiekerTraceFolder.listFiles()[0];
      reader.readAggregatedData(kiekerFolder);
   }

   private void writeFakeMeasurements(final File kiekerTraceFolder, boolean hasModifier) throws IOException {
      final Configuration configuration = new Configuration();
      configuration.setProperty(AggregatedTreeWriter.CONFIG_PATH, kiekerTraceFolder.getAbsolutePath());
      // configuration.setProperty(AggregatedTreeWriter.CONFIG_WARMUP, 0);
      final AggregatedTreeWriter writer = new AggregatedTreeWriter(configuration);
      writer.onStarting();
      String operationSignature = hasModifier ? "public void A.parent()" : "void A.parent()";
      writer.writeMonitoringRecord(new OperationExecutionRecord(operationSignature, "1", 1, 1000, 2000, "xyz", 0, 0));
      for (int i = 0; i < 100; i++) {
         writer.writeMonitoringRecord(new OperationExecutionRecord("public void A.child1()", "1", 1, 1000, 2000, "xyz", 1, 1));
         writer.writeMonitoringRecord(new OperationExecutionRecord("public void A.child2()", "1", 1, 1000, 2000, "xyz", 2, 1));
         writer.writeMonitoringRecord(new OperationExecutionRecord("public void A.child3()", "1", 1, 1000, 2000, "xyz", 3, 1));
      }
      writer.onTerminating();
   }

   private Set<CallTreeNode> buildTree() {
      final Set<CallTreeNode> includedNodes = new HashSet<>();

      includedNodes.add(root.appendChild("A.child1()", "public void A.child1()", null));
      includedNodes.add(root.appendChild("A.child2()", "public void A.child2()", null));
      includedNodes.add(root.appendChild("A.child3()", "public void A.child3()", null));
      includedNodes.add(root.appendChild(CauseSearchData.ADDED, CauseSearchData.ADDED, "public void A.child4()"));
      includedNodes.add(root);

      for (final CallTreeNode node : includedNodes) {
         node.initCommitData();
         node.setOtherKiekerPattern(node.getKiekerPattern());
      }
      return includedNodes;
   }

   private File getKiekerTraceFolder() throws IOException {
      final File kiekerTraceFolder = new File("target/kiekerreading");
      if (kiekerTraceFolder.exists()) {
         FileUtils.deleteDirectory(kiekerTraceFolder);
      }
      kiekerTraceFolder.mkdirs();
      return kiekerTraceFolder;
   }

}
