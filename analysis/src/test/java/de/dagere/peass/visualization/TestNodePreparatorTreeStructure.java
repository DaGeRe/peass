package de.dagere.peass.visualization;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.serialization.MeasuredNode;
import de.dagere.peass.measurement.rca.serialization.MeasuredValues;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;

public class TestNodePreparatorTreeStructure {
   
   private final CauseSearchData csd = new CauseSearchData();
   private final MeasuredNode rootNode = new MeasuredNode("Call#testA", "public void Call.testA()", "public void Call.testA()");
   
   @BeforeEach
   public void init() {
      csd.setConfig(new MeasurementConfig(15));
      rootNode.setStatistic(new TestcaseStatistic(5.0, 6.0, 0.01, 0.01, 15, -3, 0.001, true, 10, 10));
      rootNode.setValues(new MeasuredValues());
      rootNode.setValuesPredecessor(new MeasuredValues());
      csd.setNodes(rootNode);
   }

   @Test
   public void testColorSetting(){
      NodePreparator preparator = new NodePreparator(csd);
      preparator.prepare();
      
      GraphNode root = preparator.getGraphRoot();
      
      Assert.assertEquals(NodePreparator.COLOR_SLOWER, root.getColor());
   }
   
   @Test
   public void testColorSettingRemoved(){
      final MeasuredNode childNode = new MeasuredNode("Call#myMethod", CauseSearchData.ADDED, "public void Call.myMethod()");
      childNode.setStatistic(new TestcaseStatistic(5.0, Double.NaN, 0.01, Double.NaN, 15, -3, 0.001, true, 10, 0));
      childNode.setValues(new MeasuredValues());
      childNode.setValuesPredecessor(new MeasuredValues());
      
      rootNode.getChildren().add(childNode);
      
      csd.setNodes(rootNode);
      NodePreparator preparator = new NodePreparator(csd);
      preparator.prepare();
      
      GraphNode root = preparator.getGraphRoot();
      
      Assert.assertEquals(NodePreparator.COLOR_SLOWER, root.getColor());
      Assert.assertEquals(NodePreparator.COLOR_FASTER, root.getChildren().get(0).getColor());
   }
   
   @Test
   public void testColorSettingAdded(){
      final MeasuredNode childNode = new MeasuredNode("Call#myMethod", "public void Call.myMethod()", CauseSearchData.ADDED);
      childNode.setStatistic(new TestcaseStatistic(Double.NaN, 6.0, Double.NaN, 0.01, 15, -3, 0.001, true, 0, 10));
      childNode.setValues(new MeasuredValues());
      childNode.setValuesPredecessor(new MeasuredValues());
      rootNode.getChildren().add(childNode);
      
      csd.setNodes(rootNode);
      NodePreparator preparator = new NodePreparator(csd);
      preparator.prepare();
      
      GraphNode root = preparator.getGraphRoot();
      
      Assert.assertEquals(NodePreparator.COLOR_SLOWER, root.getColor());
      Assert.assertEquals(NodePreparator.COLOR_SLOWER, root.getChildren().get(0).getColor());
   }
}
