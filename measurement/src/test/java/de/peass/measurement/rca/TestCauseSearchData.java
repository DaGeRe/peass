package de.peass.measurement.rca;

import org.junit.Assert;
import org.junit.Test;

import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.measurement.rca.serialization.MeasuredNode;

public class TestCauseSearchData {
   
   @Test
   public void testParent() throws Exception {
      CauseSearchData csd = new CauseSearchData();
      
      MeasuredNode aMeasured = new MeasuredNode("A", "public void A.a()");
      MeasuredNode bMeasured = new MeasuredNode("B", "public void B.b()");
      aMeasured.getChildren().add(bMeasured);
      
      csd.setNodes(aMeasured);
      
      CallTreeNode aStructure = new CallTreeNode("A", "public void A.a()");
      CallTreeNode bStructure = aStructure.appendChild("B", "public void B.b()");
      CallTreeNode cStructure = buildAdditionalNode(bStructure);
      
      System.out.println(cStructure.getPosition());
      
      final MeasuredNode diff = csd.addDiff(cStructure);
      Assert.assertNotNull(diff);
   }

   private CallTreeNode buildAdditionalNode(CallTreeNode bStructure) {
      CallTreeNode cStructure = bStructure.appendChild("C", "public void C.c()");
      cStructure.setOtherVersionNode(new CallTreeNode("C", "public void C.c()"));
      cStructure.setVersions("1", "2");
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
