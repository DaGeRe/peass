package de.dagere.peass.measurement.rca.kiekerReading;


import org.junit.Assert;
import org.junit.jupiter.api.Test;


public class TestChangedTreeEOIs {
   
   @Test 
   public void testEOIs() {
      ChangedTreeBuilder treeBuilder = new ChangedTreeBuilder();
      
      Assert.assertEquals(0, treeBuilder.getRoot().getEoi(ChangedTreeBuilder.VERSION0));
      Assert.assertEquals(2, treeBuilder.getA1().getEoi(ChangedTreeBuilder.VERSION0));
      Assert.assertEquals(-1, treeBuilder.getA1_B1_constructor().getEoi(ChangedTreeBuilder.VERSION0));
      Assert.assertEquals(-1, treeBuilder.getA1_B1().getEoi(ChangedTreeBuilder.VERSION0));
      Assert.assertEquals(3, treeBuilder.getB1_constructor().getEoi(ChangedTreeBuilder.VERSION0));
      Assert.assertEquals(4, treeBuilder.getB1().getEoi(ChangedTreeBuilder.VERSION0));
      
      Assert.assertEquals(0, treeBuilder.getRoot().getEoi(ChangedTreeBuilder.VERSION1));
      Assert.assertEquals(2, treeBuilder.getA1().getEoi(ChangedTreeBuilder.VERSION1));
      Assert.assertEquals(3, treeBuilder.getA1_B1_constructor().getEoi(ChangedTreeBuilder.VERSION1));
      Assert.assertEquals(4, treeBuilder.getA1_B1().getEoi(ChangedTreeBuilder.VERSION1));
      Assert.assertEquals(5, treeBuilder.getB1_constructor().getEoi(ChangedTreeBuilder.VERSION1));
      Assert.assertEquals(6, treeBuilder.getB1().getEoi(ChangedTreeBuilder.VERSION1));
   }
}
