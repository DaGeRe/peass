package de.dagere.peass.measurement.rca;


import org.junit.Assert;
import org.junit.Test;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.treeanalysis.TreeUtil;

public class TreeUtilTest {
   

   private final CallTreeNode parent1 = new CallTreeNode("Test1#test", "public void Test1.test()", null, (MeasurementConfig) null);
   private final CallTreeNode parent2 = new CallTreeNode("Test1#test", "public void Test1.test()", null, (MeasurementConfig) null);
   
   @Test
   public void testAddedMapping()  {
      createBasicTree();
      
      parent2.appendChild("ClassA#G", "public void ClassA.G()", null);
      parent2.appendChild("ClassA#I", "public void ClassA.I()", null);
      parent2.appendChild("ClassA#J", "public void ClassA.J()", null);
      parent2.appendChild("ClassA#H", "public void ClassA.H()", null);
      parent2.appendChild("ClassA#K", "public void ClassA.K()", null);
      
      TreeUtil.findChildMapping(parent1, parent2);
      
      Assert.assertEquals(parent1.getChildren().get(0).getOtherKiekerPattern(), parent2.getChildren().get(0).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(1).getOtherKiekerPattern(), parent2.getChildren().get(1).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(2).getOtherKiekerPattern(), parent2.getChildren().get(2).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(3).getOtherKiekerPattern(), parent2.getChildren().get(3).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(4).getOtherKiekerPattern(), parent2.getChildren().get(4).getKiekerPattern());
   }
   
   @Test
   public void testNondeterministicMapping()  {
      createBasicTree();
      
      parent2.appendChild("ClassA#G", "public void ClassA.G()", null);
      parent2.appendChild("ClassA#I", "public void ClassA.I()", null);
      parent2.appendChild("ClassA#J", "public void ClassA.J()", null);
      
      TreeUtil.findChildMapping(parent1, parent2);
      
      Assert.assertEquals(parent1.getChildren().get(0).getOtherKiekerPattern(), parent2.getChildren().get(0).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(1).getOtherKiekerPattern(), parent2.getChildren().get(1).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(2).getOtherKiekerPattern(), parent2.getChildren().get(2).getKiekerPattern());
   }
   
   @Test
   public void testVisibilityChangeMapping() {
      createBasicTree();
      
      parent2.appendChild("ClassA#methodA1", "public void ClassA.methodA1()", null);
      parent2.appendChild("ClassA#methodA2", "void ClassA.methodA2()", null);
      parent2.appendChild("ClassA#methodA3", "public void ClassA.methodA3()", null);
      
      TreeUtil.findChildMapping(parent1, parent2);
      
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(0).getOtherKiekerPattern(), parent2.getChildren().get(0).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(1).getOtherKiekerPattern(), parent2.getChildren().get(1).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(2).getOtherKiekerPattern(), parent2.getChildren().get(2).getKiekerPattern());
   }
   
   @Test
   public void testParameterChangeMapping() {
      createBasicTree();
      
      parent2.appendChild("ClassA#methodA1", "public void ClassA.methodA1()", null);
      parent2.appendChild("ClassA#methodA2", "public void ClassA.methodA2(int a)", null);
      parent2.appendChild("ClassA#methodA3", "public void ClassA.methodA3()", null);
      
      TreeUtil.findChildMapping(parent1, parent2);
      
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(0).getOtherKiekerPattern(), parent2.getChildren().get(0).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(1).getOtherKiekerPattern(), parent2.getChildren().get(1).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(2).getOtherKiekerPattern(), parent2.getChildren().get(2).getKiekerPattern());
   }
   
   @Test
   public void testSwitchMapping() {
      createBasicTree();
      
      parent2.appendChild("ClassA#methodA1", "public void ClassA.methodA1()", null);
      parent2.appendChild("ClassA#methodA3", "public void ClassA.methodA3()", null);
      parent2.appendChild("ClassA#methodA2", "public void ClassA.methodA2(int a)", null);
      
      TreeUtil.findChildMapping(parent1, parent2);
      
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(0).getOtherKiekerPattern(), parent2.getChildren().get(0).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(1).getOtherKiekerPattern(), parent2.getChildren().get(2).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(2).getOtherKiekerPattern(), parent2.getChildren().get(1).getKiekerPattern());
   }
   
   @Test
   public void testAddition() {
      createBasicTree();
      
      parent2.appendChild("ClassA#methodA1", "public void ClassA.methodA1()", null);
      parent2.appendChild("ClassA#methodA2", "void ClassA.methodA2()", null);
      parent2.appendChild("ClassA#methodA3", "public void ClassA.methodA3()", null);
      parent2.appendChild("ClassA#methodA4", "public void ClassA.methodA4()", null);
      
      TreeUtil.findChildMapping(parent1, parent2);
      
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(0).getOtherKiekerPattern(), parent2.getChildren().get(0).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(1).getOtherKiekerPattern(), parent2.getChildren().get(1).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(2).getOtherKiekerPattern(), parent2.getChildren().get(2).getKiekerPattern());
   }
   
   @Test
   public void testOverriding() {
      parent1.appendChild("ClassA#methodA1", "public void ClassA.methodA1(int)", null);
      parent1.appendChild("ClassA#methodA1", "public void ClassA.methodA1(byte)", null);
      parent1.appendChild("ClassA#methodA1", "public void ClassA.methodA1(String)", null);
      parent1.appendChild("ClassA#methodA1", "public void ClassA.methodA1()", null);
      
      parent2.appendChild("ClassA#methodA1", "public void ClassA.methodA1(int, String)", null);
      parent2.appendChild("ClassA#methodA1", "public void ClassA.methodA1(byte, double)", null);
      parent2.appendChild("ClassA#methodA1", "public void ClassA.methodA1()", null);
      parent2.appendChild("ClassA#methodA1", "public void ClassA.methodA1(String)", null);
      
      TreeUtil.findChildMapping(parent1, parent2);
      
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(0).getOtherKiekerPattern(), parent2.getChildren().get(0).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(1).getOtherKiekerPattern(), parent2.getChildren().get(1).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(2).getOtherKiekerPattern(), parent2.getChildren().get(3).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(3).getOtherKiekerPattern(), parent2.getChildren().get(2).getKiekerPattern());
   }
   
   @Test
   public void testSuperclass() {
      parent1.appendChild("ClassA#methodA1", "public void ClassA.methodA1()", null);
      parent1.appendChild("ClassA#methodA2", "public void ClassA.methodA2()", null);
      
      parent2.appendChild("ClassB#methodA2", "public void ClassB.methodA2()", null);
      parent2.appendChild("ClassA#methodZ1", "public void ClassA.methodZ1()", null);
      
      TreeUtil.findChildMapping(parent1, parent2);
      
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(0).getOtherKiekerPattern(), parent2.getChildren().get(1).getKiekerPattern());
      Assert.assertEquals(parent1.getChildren().get(1).getOtherKiekerPattern(), parent2.getChildren().get(0).getKiekerPattern());
   }
   
   private void createBasicTree() {
      parent1.appendChild("ClassA#methodA1", "public void ClassA.methodA1()", null);
      parent1.appendChild("ClassA#methodA2", "public void ClassA.methodA2()", null);
      parent1.appendChild("ClassA#methodA3", "public void ClassA.methodA3()", null);
   }
}
