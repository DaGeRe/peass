package de.dagere.peass.measurement.rca.data;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.measurement.rca.helper.TreeBuilder;
import de.dagere.peass.measurement.rca.helper.TreeBuilderBig;

public class TestCallTreeNode {
   @Test
   public void testToEntity() {
      final CallTreeNode node = new CallTreeNode("de.mypackage.Test#callMethod", "public void de.mypackage.Test.callMethod()", "public void de.mypackage.Test.callMethod()", (MeasurementConfig) null);
      Assert.assertEquals(new ChangedEntity("de.mypackage.Test", "", "callMethod"), node.toEntity());
      Assert.assertEquals("de.mypackage.Test#callMethod", node.toEntity().toString());
      Assert.assertEquals(0, node.toEntity().getParameterTypes().length);
   }
   
   @Test
   public void testToEntityParameter() {
      final CallTreeNode node = new CallTreeNode("moduleA" + ChangedEntity.MODULE_SEPARATOR + "de.mypackage.Test#callMethod", "public void "+ ChangedEntity.MODULE_SEPARATOR + "de.mypackage.Test.callMethod(int)", null, (MeasurementConfig) null);
      ChangedEntity entity = node.toEntity();
      Assert.assertEquals(new ChangedEntity("de.mypackage.Test", "moduleA", "callMethod"), entity);
      Assert.assertEquals("moduleA" + ChangedEntity.MODULE_SEPARATOR + "de.mypackage.Test#callMethod(int)", entity.toString());
      Assert.assertEquals("int", entity.getParameterTypes()[0]);
   }

   @Test
   public void testPosition() throws Exception {
      CallTreeNode aStructure = new CallTreeNode("A", "public void A.a()", "public void A.a()", (MeasurementConfig) null);
      CallTreeNode bStructure = aStructure.appendChild("B", "public void B.b()", "public void B.b()");
      CallTreeNode cStructure = bStructure.appendChild("C", "public void C.c()", "public void C.c()");
      Assert.assertEquals(0, bStructure.getPosition());
      Assert.assertEquals(0, cStructure.getPosition());
   }

   @Test
   public void testEOI() {
      TreeBuilderBig builder = new TreeBuilderBig(false);

      Assert.assertEquals(0, builder.getRoot().getEoi(TreeBuilder.VERSION_OLD));
      Assert.assertEquals(1, builder.getA().getEoi(TreeBuilder.VERSION_OLD));
      Assert.assertEquals(2, builder.getB().getEoi(TreeBuilder.VERSION_OLD));
      Assert.assertEquals(3, builder.getC().getEoi(TreeBuilder.VERSION_OLD));
      Assert.assertEquals(4, builder.getB2().getEoi(TreeBuilder.VERSION_OLD));
   }

   @Test
   public void testModuleSeparation() {
      String call = "moduleA" + ChangedEntity.MODULE_SEPARATOR + "ClazzA" + ChangedEntity.METHOD_SEPARATOR + "test";
      CallTreeNode node = new CallTreeNode(call, "public void " + call.replace("#", ".") +"()", null, new MeasurementConfig(1));
      
      Assert.assertEquals(node.getCall(), "ClazzA" + ChangedEntity.METHOD_SEPARATOR + "test");
      Assert.assertEquals(node.getModule(), "moduleA");
   }
}
