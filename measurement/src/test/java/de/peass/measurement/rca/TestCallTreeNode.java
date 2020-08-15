package de.peass.measurement.rca;

import org.junit.Assert;
import org.junit.Test;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.helper.TreeBuilderBig;

public class TestCallTreeNode {
   @Test
   public void testToEntity() {
      final CallTreeNode node = new CallTreeNode("de.mypackage.Test#callMethod", "public void de.mypackage.Test.callMethod()");
      Assert.assertEquals(new ChangedEntity("de.mypackage.Test", "", "callMethod"), node.toEntity());
      Assert.assertEquals("de.mypackage.Test#callMethod", node.toEntity().toString());
   }
   
   @Test
   public void testPosition() throws Exception {
      CallTreeNode aStructure = new CallTreeNode("A", "public void A.a()");
      CallTreeNode bStructure = aStructure.appendChild("B", "public void B.b()");
      CallTreeNode cStructure = bStructure.appendChild("C", "public void C.c()");
      Assert.assertEquals(0, bStructure.getPosition());
      Assert.assertEquals(0, cStructure.getPosition());
   }
   
   @Test
   public void testEOI() {
      TreeBuilderBig builder = new TreeBuilderBig(false);
      
      Assert.assertEquals(0, builder.getRoot().getEoi());
      Assert.assertEquals(1, builder.getA().getEoi());
      Assert.assertEquals(2, builder.getB().getEoi());
      Assert.assertEquals(3, builder.getC().getEoi());
      Assert.assertEquals(4, builder.getB2().getEoi());
   }
}
