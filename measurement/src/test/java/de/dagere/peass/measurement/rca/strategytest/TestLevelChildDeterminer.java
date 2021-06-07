package de.dagere.peass.measurement.rca.strategytest;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.helper.TreeBuilder;
import de.dagere.peass.measurement.rca.searcher.LevelChildDeterminer;

public class TestLevelChildDeterminer {
   
   private static final TreeBuilder builder = new TreeBuilder();
   
   @Test
   public void testZeroLevels() {
      CallTreeNode root = builder.getRoot();
      LevelChildDeterminer determiner = new LevelChildDeterminer(Arrays.asList(root), 0);
      Assert.assertEquals(0, determiner.getOnlyChildNodes().size());
      Assert.assertEquals(1, determiner.getSelectedIncludingParentNodes().size());
   }
   
   @Test
   public void testOneLevel() {
      CallTreeNode root = builder.getRoot();
      LevelChildDeterminer determiner = new LevelChildDeterminer(Arrays.asList(root), 1);
      Assert.assertEquals(3, determiner.getOnlyChildNodes().size());
      Assert.assertEquals(4, determiner.getSelectedIncludingParentNodes().size());
   }
   
   @Test
   public void testTwoLevels() {
      CallTreeNode root = builder.getRoot();
      LevelChildDeterminer determiner = new LevelChildDeterminer(Arrays.asList(root), 2);
      Assert.assertEquals(4, determiner.getOnlyChildNodes().size());
      Assert.assertEquals(5, determiner.getSelectedIncludingParentNodes().size());
   }
   
   @Test
   public void testTwoParents() {
      LevelChildDeterminer determiner = new LevelChildDeterminer(Arrays.asList(builder.getA(), builder.getC(), builder.getConstructor()), 2);
      Assert.assertEquals(1, determiner.getOnlyChildNodes().size());
      Assert.assertEquals(4, determiner.getSelectedIncludingParentNodes().size());
   }
}
