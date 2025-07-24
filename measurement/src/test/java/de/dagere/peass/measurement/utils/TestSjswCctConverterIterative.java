package de.dagere.peass.measurement.utils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.CauseSearcherConfigMixin;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.utils.sjsw.SjswCctConverter;
import de.dagere.peass.utils.Constants;
import io.github.terahidro2003.cct.result.StackTraceTreeNode;
import io.github.terahidro2003.cct.result.VmMeasurement;

public class TestSjswCctConverterIterative {

   private static final String COMMIT = "f0729636eaa6e0f6f39b663b5231a8b28142731e";
   private static final String PREDECESSOR = "5a9a3a0763ca8103f0b9b267876eaa42082a78aa";

   @Test
   public void testConversion() throws IOException {
      File folder = new File("src/test/resources/samplingTestData/iterative-sampling/");
      File currentFile = new File(folder, "f0729636eaa6e0f6f39b663b5231a8b28142731e.json");
      StackTraceTreeNode current = Constants.OBJECTMAPPER.readValue(currentFile, StackTraceTreeNode.class);

      File predecessorFile = new File(folder, "5a9a3a0763ca8103f0b9b267876eaa42082a78aa.json");
      StackTraceTreeNode predecessor = Constants.OBJECTMAPPER.readValue(predecessorFile, StackTraceTreeNode.class);

      executeSjswCctConversion(current, predecessor);
   }
   
   @Test
   public void testConversionWithFewerData() throws IOException {
      File folder = new File("src/test/resources/samplingTestData/iterative-sampling/");
      File currentFile = new File(folder, "f0729636eaa6e0f6f39b663b5231a8b28142731e.json");
      StackTraceTreeNode current = Constants.OBJECTMAPPER.readValue(currentFile, StackTraceTreeNode.class);

      File predecessorFile = new File(folder, "5a9a3a0763ca8103f0b9b267876eaa42082a78aa.json");
      StackTraceTreeNode predecessor = Constants.OBJECTMAPPER.readValue(predecessorFile, StackTraceTreeNode.class);

      StackTraceTreeNode firstChild = predecessor.getChildren().get(0);
      List<VmMeasurement> vmMeasurements = firstChild.getVmMeasurements().get(PREDECESSOR);
      vmMeasurements.remove(1);
      vmMeasurements.remove(1);
      
      executeSjswCctConversion(current, predecessor);
   }

   private void executeSjswCctConversion(StackTraceTreeNode current, StackTraceTreeNode predecessor) {
      MeasurementConfig config = new MeasurementConfig(3, COMMIT, PREDECESSOR);
      config.setIterations(20);
      config.setUseIterativeSampling(true);
      SjswCctConverter sjswCctConverter = new SjswCctConverter(COMMIT, PREDECESSOR,
            config);
      CallTreeNode rootNode = sjswCctConverter.convertToCCT(current, predecessor);

      Assert.assertEquals(rootNode.getCall(), "de.dagere.peass.MainTest#testMe");
      SjswCctConverterTest.printCallTreeNode(rootNode);

      System.out.println();
      System.out.println();

      Assert.assertEquals(rootNode.getOtherCommitNode().getCall(), "de.dagere.peass.MainTest#testMe");
      SjswCctConverterTest.printCallTreeNode(rootNode.getOtherCommitNode());

      CauseSearchData causeSearchData = new CauseSearchData(config, new CauseSearcherConfig(null, new CauseSearcherConfigMixin()));
      causeSearchData.addDiff(rootNode);
      causeSearchData.addDetailDiff(rootNode);

      addMeasurements(rootNode, causeSearchData, causeSearchData);
   }

   public void addMeasurements(CallTreeNode parent, CauseSearchData data, CauseSearchData dataDetails) {
      for (CallTreeNode child : parent.getChildren()) {
         data.addDiff(child);
         dataDetails.addDetailDiff(child);
         addMeasurements(child, data, dataDetails);
      }
   }

}
