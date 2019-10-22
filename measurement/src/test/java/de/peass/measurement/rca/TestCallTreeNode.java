package de.peass.measurement.rca;

import org.junit.Assert;
import org.junit.Test;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.measurement.rca.data.CallTreeNode;

public class TestCallTreeNode {
   @Test
   public void testToEntity() {
      final CallTreeNode node = new CallTreeNode("de.mypackage.Test#callMethod", "public void de.mypackage.Test.callMethod()");
      Assert.assertEquals(new ChangedEntity("de.mypackage.Test", "", "callMethod"), node.toEntity());
      Assert.assertEquals("de.mypackage.Test#callMethod", node.toEntity().toString());
   }
}
