package de.dagere.peass.measurement.rca.data;

import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.serialization.MeasuredNode;

public class TestCauseSearchData {
   
   @Test
   public void testParent() throws Exception {
      CauseSearchData csd = new CauseSearchData();
      MeasurementConfig config = new MeasurementConfig(-1);
      config.getExecutionConfig().setVersion("2");
      config.getExecutionConfig().setVersionOld("1");
      csd.setConfig(config);
      
      MeasuredNode aMeasured = new MeasuredNode("A", "public void A.a()", null);
      MeasuredNode bMeasured = new MeasuredNode("B", "public void B.b()", null);
      aMeasured.getChildren().add(bMeasured);
      
      csd.setNodes(aMeasured);
      
      CallTreeNode aStructure = new CallTreeNode("A", "public void A.a()", null, config);
      CallTreeNode bStructure = aStructure.appendChild("B", "public void B.b()", null);
      CallTreeNode cStructure = buildAdditionalNode(bStructure);
      
      System.out.println(cStructure.getPosition());
      
      final MeasuredNode diff = csd.addDiff(cStructure);
      Assert.assertNotNull(diff);
   }

   private CallTreeNode buildAdditionalNode(final CallTreeNode bStructure) {
      CallTreeNode cStructure = bStructure.appendChild("C", "public void C.c()", null);
      cStructure.setOtherKiekerPattern("public void C.c()");
      cStructure.initVersions();
      for (int i = 0; i < 3; i++) {
         cStructure.newVM("1");
         cStructure.addMeasurement("1", 15L);
         cStructure.addMeasurement("1", 15L);
         cStructure.newVM("2");
         cStructure.addMeasurement("2", 15L);
         cStructure.addMeasurement("2", 15L);
      }
      cStructure.createStatistics("1");
      cStructure.createStatistics("2");
      return cStructure;
   }
}
