package de.peass.measurement.searchcause;


import org.junit.Assert;
import org.junit.Test;

import de.peass.measurement.searchcause.data.CallTreeNode;
import de.peass.measurement.searchcause.treeanalysis.TreeUtil;

public class TreeUtilTest {
   
   @Test
   public void testNondeterministicMapping()  {
      final CallTreeNode parent1 = createBasicTree();
      
      final CallTreeNode parent2 = new CallTreeNode("Test1#test", "public void Test1.test", null);
      parent2.appendChild("ClassA#G", "public void ClassA.G");
      parent2.appendChild("ClassA#I", "public void ClassA.I");
      parent2.appendChild("ClassA#J", "public void ClassA.J");
      
      TreeUtil.findMapping(parent1, parent2);
      
      Assert.assertEquals(parent1.getChildren().get(0).getOtherVersionNode(), parent2.getChildren().get(0));
      Assert.assertEquals(parent1.getChildren().get(1).getOtherVersionNode(), parent2.getChildren().get(1));
      Assert.assertEquals(parent1.getChildren().get(2).getOtherVersionNode(), parent2.getChildren().get(2));
   }
   
   @Test
   public void testVisibilityChangeMapping() {
      final CallTreeNode parent1 = createBasicTree();
      
      final CallTreeNode parent2 = new CallTreeNode("Test1#test", "public void Test1.test", null);
      parent2.getChildren().add(new CallTreeNode("ClassA#methodA1", "public void ClassA.methodA1()", parent2));
      parent2.getChildren().add(new CallTreeNode("ClassA#methodA2", "void ClassA.methodA2()", parent2));
      parent2.getChildren().add(new CallTreeNode("ClassA#methodA3", "public void ClassA.methodA3()", parent2));
      
      TreeUtil.findMapping(parent1, parent2);
      
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherVersionNode());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherVersionNode());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherVersionNode());
      Assert.assertEquals(parent1.getChildren().get(0).getOtherVersionNode(), parent2.getChildren().get(0));
      Assert.assertEquals(parent1.getChildren().get(1).getOtherVersionNode(), parent2.getChildren().get(1));
      Assert.assertEquals(parent1.getChildren().get(2).getOtherVersionNode(), parent2.getChildren().get(2));
   }
   
   @Test
   public void testParameterChangeMapping() {
      final CallTreeNode parent1 = createBasicTree();
      
      final CallTreeNode parent2 = new CallTreeNode("Test1#test", "public void Test1.test", null);
      parent2.getChildren().add(new CallTreeNode("ClassA#methodA1", "public void ClassA.methodA1()", parent2));
      parent2.getChildren().add(new CallTreeNode("ClassA#methodA2", "public void ClassA.methodA2(int a)", parent2));
      parent2.getChildren().add(new CallTreeNode("ClassA#methodA3", "public void ClassA.methodA3()", parent2));
      
      TreeUtil.findMapping(parent1, parent2);
      
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherVersionNode());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherVersionNode());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherVersionNode());
      Assert.assertEquals(parent1.getChildren().get(0).getOtherVersionNode(), parent2.getChildren().get(0));
      Assert.assertEquals(parent1.getChildren().get(1).getOtherVersionNode(), parent2.getChildren().get(1));
      Assert.assertEquals(parent1.getChildren().get(2).getOtherVersionNode(), parent2.getChildren().get(2));
   }
   
   @Test
   public void testSwitchMapping() {
      final CallTreeNode parent1 = createBasicTree();
      
      final CallTreeNode parent2 = new CallTreeNode("Test1#test", "public void Test1.test", null);
      parent2.getChildren().add(new CallTreeNode("ClassA#methodA1", "public void ClassA.methodA1()", parent2));
      parent2.getChildren().add(new CallTreeNode("ClassA#methodA3", "public void ClassA.methodA3()", parent2));
      parent2.getChildren().add(new CallTreeNode("ClassA#methodA2", "public void ClassA.methodA2(int a)", parent2));
      
      TreeUtil.findMapping(parent1, parent2);
      
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherVersionNode());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherVersionNode());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherVersionNode());
      Assert.assertEquals(parent1.getChildren().get(0).getOtherVersionNode(), parent2.getChildren().get(0));
      Assert.assertEquals(parent1.getChildren().get(1).getOtherVersionNode(), parent2.getChildren().get(2));
      Assert.assertEquals(parent1.getChildren().get(2).getOtherVersionNode(), parent2.getChildren().get(1));
   }
   
   @Test
   public void testAddition() {
      final CallTreeNode parent1 = createBasicTree();
      
      final CallTreeNode parent2 = new CallTreeNode("Test1#test", "public void Test1.test", null);
      parent2.getChildren().add(new CallTreeNode("ClassA#methodA1", "public void ClassA.methodA1()", parent2));
      parent2.getChildren().add(new CallTreeNode("ClassA#methodA2", "void ClassA.methodA2()", parent2));
      parent2.getChildren().add(new CallTreeNode("ClassA#methodA3", "public void ClassA.methodA3()", parent2));
      parent2.getChildren().add(new CallTreeNode("ClassA#methodA4", "public void ClassA.methodA4()", parent2));
      
      TreeUtil.findMapping(parent1, parent2);
      
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherVersionNode());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherVersionNode());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherVersionNode());
      Assert.assertEquals(parent1.getChildren().get(0).getOtherVersionNode(), parent2.getChildren().get(0));
      Assert.assertEquals(parent1.getChildren().get(1).getOtherVersionNode(), parent2.getChildren().get(1));
      Assert.assertEquals(parent1.getChildren().get(2).getOtherVersionNode(), parent2.getChildren().get(2));
   }
   
   @Test
   public void testOverriding() {
      final CallTreeNode parent1 = new CallTreeNode("Test1#test", "public void Test1.test", null);
      parent1.getChildren().add(new CallTreeNode("ClassA#methodA1", "public void ClassA.methodA1(int)", parent1));
      parent1.getChildren().add(new CallTreeNode("ClassA#methodA1", "public void ClassA.methodA1(byte)", parent1));
      parent1.getChildren().add(new CallTreeNode("ClassA#methodA1", "public void ClassA.methodA1(String)", parent1));
      parent1.getChildren().add(new CallTreeNode("ClassA#methodA1", "public void ClassA.methodA1()", parent1));
      
      final CallTreeNode parent2 = new CallTreeNode("Test1#test", "public void Test1.test", null);
      parent2.getChildren().add(new CallTreeNode("ClassA#methodA1", "public void ClassA.methodA1(int, String)", parent2));
      parent2.getChildren().add(new CallTreeNode("ClassA#methodA1", "public void ClassA.methodA1(byte, double)", parent2));
      parent2.getChildren().add(new CallTreeNode("ClassA#methodA1", "public void ClassA.methodA1()", parent2));
      parent2.getChildren().add(new CallTreeNode("ClassA#methodA1", "public void ClassA.methodA1(String)", parent2));
      
      TreeUtil.findMapping(parent1, parent2);
      
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherVersionNode());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherVersionNode());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherVersionNode());
      Assert.assertEquals(parent1.getChildren().get(0).getOtherVersionNode(), parent2.getChildren().get(0));
      Assert.assertEquals(parent1.getChildren().get(1).getOtherVersionNode(), parent2.getChildren().get(1));
      Assert.assertEquals(parent1.getChildren().get(2).getOtherVersionNode(), parent2.getChildren().get(3));
      Assert.assertEquals(parent1.getChildren().get(3).getOtherVersionNode(), parent2.getChildren().get(2));
   }
   
   @Test
   public void testSuperclass() {
      final CallTreeNode parent1 = new CallTreeNode("Test1#test", "public void Test1.test", null);
      parent1.appendChild("ClassA#methodA1", "public void ClassA.methodA1()");
      parent1.appendChild("ClassA#methodA2", "public void ClassA.methodA2()");
      
      final CallTreeNode parent2 = new CallTreeNode("Test1#test", "public void Test1.test", null);
      parent2.appendChild("ClassB#methodA2", "public void ClassB.methodA2()");
      parent2.appendChild("ClassA#methodZ1", "public void ClassA.methodZ1()");
      
      TreeUtil.findMapping(parent1, parent2);
      
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherVersionNode());
      Assert.assertNotNull(parent1.getChildren().get(0).getOtherVersionNode());
      Assert.assertEquals(parent1.getChildren().get(0).getOtherVersionNode(), parent2.getChildren().get(1));
      Assert.assertEquals(parent1.getChildren().get(1).getOtherVersionNode(), parent2.getChildren().get(0));
   }
   
   private CallTreeNode createBasicTree() {
      final CallTreeNode parent1 = new CallTreeNode("Test1#test", "public void Test1.test", null);
      parent1.appendChild("ClassA#methodA1", "public void ClassA.methodA1()");
      parent1.appendChild("ClassA#methodA2", "public void ClassA.methodA2()");
      parent1.appendChild("ClassA#methodA3", "public void ClassA.methodA3()");
      return parent1;
   }
}
