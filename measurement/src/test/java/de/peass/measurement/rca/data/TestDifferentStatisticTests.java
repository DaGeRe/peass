package de.peass.measurement.rca.data;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.peass.config.ImplementedTests;
import de.peass.measurement.analysis.Relation;
import de.peass.statistics.StatisticUtil;
import de.precision.analysis.repetitions.bimodal.CompareData;

public class TestDifferentStatisticTests {
   
   @BeforeEach
   public void init() {
      TestCallTreeStatistics.CONFIG.setVersionOld("B");
      TestCallTreeStatistics.CONFIG.setVersion("A");
   }
   
   @Test
   public void testBimodalExample() {
      final CallTreeNode node = new CallTreeNode("de.mypackage.Test#callMethod", "public void de.mypackage.Test.callMethod()", "public void de.mypackage.Test.callMethod()", TestCallTreeStatistics.CONFIG);
      final CallTreeNode otherVersionNode = new CallTreeNode("de.mypackage.Test#callMethod", "public void de.mypackage.Test.callMethod()", "public void de.mypackage.Test.callMethod()", TestCallTreeStatistics.CONFIG);
      node.setOtherVersionNode(otherVersionNode);
      
      buildBimodalMeasurementValues(node);
      
      CompareData cd = node.getComparableStatistics("A", "B");
      
      TestCallTreeStatistics.CONFIG.getStatisticsConfig().setStatisticTest(ImplementedTests.AGNOSTIC_T_TEST);
      Relation relationAgnostic = StatisticUtil.isDifferent(cd, TestCallTreeStatistics.CONFIG);
      Assert.assertEquals(Relation.UNKOWN, relationAgnostic);
      
      TestCallTreeStatistics.CONFIG.getStatisticsConfig().setStatisticTest(ImplementedTests.T_TEST);
      Relation relationTTest = StatisticUtil.isDifferent(cd, TestCallTreeStatistics.CONFIG);
      Assert.assertEquals(Relation.EQUAL, relationTTest);
      
      TestCallTreeStatistics.CONFIG.getStatisticsConfig().setStatisticTest(ImplementedTests.BIMODAL_T_TEST);
      Relation relation = StatisticUtil.isDifferent(cd, TestCallTreeStatistics.CONFIG);
      Assert.assertEquals(Relation.LESS_THAN, relation);
   }

   private void buildBimodalMeasurementValues(final CallTreeNode node) {
      node.initVersions();
      for (int vmIndex = 0; vmIndex < 40; vmIndex++) {
         node.newVM("A");
         node.newVM("B");
         for (int i = 0; i < 5; i++) {
            node.addMeasurement("A", 15L);
            node.addMeasurement("B", 16L);
         }
         
         node.newVM("A");
         node.newVM("B");
         for (int i = 0; i < 5; i++) {
            node.addMeasurement("A", 20L);
            node.addMeasurement("B", 21L);
         }
      }
      
      node.createStatistics("A");
      node.createStatistics("B");
   }
}
