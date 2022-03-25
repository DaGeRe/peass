package de.dagere.peass.visualization;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.serialization.MeasuredNode;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;

public class TestNodePreparatorTreeStructure {
   
   private final CauseSearchData csd = new CauseSearchData();
   private final MeasuredNode rootNode = new MeasuredNode("Call#testA", "public void Call.testA()", null);
   
   @BeforeEach
   public void init() {
      csd.setConfig(new MeasurementConfig(15));
      rootNode.setStatistic(new TestcaseStatistic(5.0, 6.0, 0.01, 0.01, 15, -3, true, 10, 10));
      csd.setNodes(rootNode);
   }

   @Test
   public void testColorSetting(){
      NodePreparator preparator = new NodePreparator(csd);
      preparator.prepare();
      
      GraphNode root = preparator.getRootNode();
      
      Assert.assertEquals(NodePreparator.COLOR_SLOWER, root.getColor());
   }
   
   @Test
   public void testColorSettingRemoved(){
      final MeasuredNode childNode = new MeasuredNode("Call#myMethod", "public void Call.myMethod()", null);
      childNode.setStatistic(new TestcaseStatistic(5.0, Double.NaN, 0.01, Double.NaN, 15, -3, true, 10, 0));
      rootNode.getChildren().add(childNode);
      
      csd.setNodes(rootNode);
      NodePreparator preparator = new NodePreparator(csd);
      preparator.prepare();
      
      GraphNode root = preparator.getRootNode();
      
      Assert.assertEquals(NodePreparator.COLOR_SLOWER, root.getColor());
      Assert.assertEquals(NodePreparator.COLOR_SLOWER, root.getChildren().get(0).getColor());
   }
   
   @Test
   public void testColorSettingAdded(){
      final MeasuredNode childNode = new MeasuredNode("Call#myMethod", "public void Call.myMethod()", null);
      childNode.setStatistic(new TestcaseStatistic(Double.NaN, 6.0, Double.NaN, 0.01, 15, -3, true, 0, 10));
      rootNode.getChildren().add(childNode);
      
      csd.setNodes(rootNode);
      NodePreparator preparator = new NodePreparator(csd);
      preparator.prepare();
      
      GraphNode root = preparator.getRootNode();
      
      Assert.assertEquals(NodePreparator.COLOR_SLOWER, root.getColor());
      Assert.assertEquals(NodePreparator.COLOR_FASTER, root.getChildren().get(0).getColor());
   }
}
