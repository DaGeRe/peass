package de.dagere.peass.measurement.utils;

import java.io.File;
import java.io.IOException;

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

public class TestSjswCctConverterIterative {

   @Test
   public void testConversion() throws IOException {
      File folder = new File("src/test/resources/samplingTestData/iterative-sampling/");
      File currentFile = new File(folder, "f0729636eaa6e0f6f39b663b5231a8b28142731e.json");
      StackTraceTreeNode current = Constants.OBJECTMAPPER.readValue(currentFile, StackTraceTreeNode.class);

      File predecessorFile = new File(folder, "5a9a3a0763ca8103f0b9b267876eaa42082a78aa.json");
      StackTraceTreeNode predecessor = Constants.OBJECTMAPPER.readValue(predecessorFile, StackTraceTreeNode.class);

      MeasurementConfig config = new MeasurementConfig(3, "f0729636eaa6e0f6f39b663b5231a8b28142731e", "5a9a3a0763ca8103f0b9b267876eaa42082a78aa");
      config.setIterations(20);
      config.setUseIterativeSampling(true);
      SjswCctConverter sjswCctConverter = new SjswCctConverter("f0729636eaa6e0f6f39b663b5231a8b28142731e", "5a9a3a0763ca8103f0b9b267876eaa42082a78aa",
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
         System.out.println(child.getCall());
         data.addDiff(child);
         dataDetails.addDetailDiff(child);
         addMeasurements(child, data, dataDetails);
      }
   }

}
