package de.peass.measurement.rca;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;

import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.measurement.rca.serialization.MeasuredNode;

public class TestCauseSearchData {
   
   @Test
   public void testParent() throws Exception {
      CauseSearchData csd = new CauseSearchData();
      
      MeasuredNode a = new MeasuredNode("A", "public void A.a()");
      MeasuredNode b = new MeasuredNode("B", "public void B.b()");
      a.getChildren().add(b);
      
      csd.setNodes(a);
      
      CallTreeNode aStructure = new CallTreeNode("A", "public void A.a()");
      CallTreeNode bStructure = aStructure.appendChild("B", "public void B.b()");
      CallTreeNode cStructure = buildAdditionalNode(bStructure);
      
      System.out.println(cStructure.getPosition());
      
      Assert.assertNotNull(csd.addDiff(cStructure));
   }

   private CallTreeNode buildAdditionalNode(CallTreeNode bStructure) {
      CallTreeNode cStructure = bStructure.appendChild("C", "public void C.c()");
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
