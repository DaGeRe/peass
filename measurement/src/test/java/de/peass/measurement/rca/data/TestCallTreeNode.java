package de.peass.measurement.rca.data;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.peass.config.MeasurementConfiguration;
import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.measurement.rca.helper.TreeBuilderBig;

public class TestCallTreeNode {
   @Test
   public void testToEntity() {
      final CallTreeNode node = new CallTreeNode("de.mypackage.Test#callMethod", "public void de.mypackage.Test.callMethod()", "public void de.mypackage.Test.callMethod()", (MeasurementConfiguration) null);
      Assert.assertEquals(new ChangedEntity("de.mypackage.Test", "", "callMethod"), node.toEntity());
      Assert.assertEquals("de.mypackage.Test#callMethod", node.toEntity().toString());
   }
   
   @Test
   public void testToEntityParameter() {
      final CallTreeNode node = new CallTreeNode("moduleA" + ChangedEntity.MODULE_SEPARATOR + "de.mypackage.Test#callMethod", "public void "+ ChangedEntity.MODULE_SEPARATOR + "de.mypackage.Test.callMethod(int)", null, (MeasurementConfiguration) null);
      ChangedEntity entity = node.toEntity();
      Assert.assertEquals(new ChangedEntity("de.mypackage.Test", "moduleA", "callMethod"), entity);
      Assert.assertEquals("moduleA" + ChangedEntity.MODULE_SEPARATOR + "de.mypackage.Test#callMethod", entity.toString());
      Assert.assertEquals("int", entity.getParameterTypes()[0]);
   }

   @Test
   public void testPosition() throws Exception {
      CallTreeNode aStructure = new CallTreeNode("A", "public void A.a()", "public void A.a()", (MeasurementConfiguration) null);
      CallTreeNode bStructure = aStructure.appendChild("B", "public void B.b()", "public void B.b()");
      CallTreeNode cStructure = bStructure.appendChild("C", "public void C.c()", "public void C.c()");
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

   @Test
   public void testModuleSeparation() {
      String call = "moduleA" + ChangedEntity.MODULE_SEPARATOR + "ClazzA" + ChangedEntity.METHOD_SEPARATOR + "test";
      CallTreeNode node = new CallTreeNode(call, "public void " + call.replace("#", ".") +"()", null, new MeasurementConfiguration(1));
      
      Assert.assertEquals(node.getCall(), "ClazzA" + ChangedEntity.METHOD_SEPARATOR + "test");
      Assert.assertEquals(node.getModule(), "moduleA");
   }
}
