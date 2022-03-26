package de.dagere.peass.measurement.rca.data;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dagere.peass.config.StatisticalTests;
import de.dagere.peass.measurement.statistics.Relation;
import de.dagere.peass.measurement.statistics.StatisticUtil;
import de.dagere.peass.measurement.statistics.bimodal.CompareData;

public class TestDifferentStatisticTests {
   
   @BeforeEach
   public void init() {
      TestCallTreeStatistics.CONFIG.getExecutionConfig().setVersionOld("B");
      TestCallTreeStatistics.CONFIG.getExecutionConfig().setVersion("A");
   }
   
   @Test
   public void testBimodalExample() {
      final CallTreeNode node = new CallTreeNode("de.mypackage.Test#callMethod", "public void de.mypackage.Test.callMethod()", "public void de.mypackage.Test.callMethod()", TestCallTreeStatistics.CONFIG);
      
      buildBimodalMeasurementValues(node);
      
      CompareData cd = node.getComparableStatistics("A", "B");
      
      TestCallTreeStatistics.CONFIG.getStatisticsConfig().setStatisticTest(StatisticalTests.AGNOSTIC_T_TEST);
      Relation relationAgnostic = StatisticUtil.isDifferent(cd, TestCallTreeStatistics.CONFIG.getStatisticsConfig());
      Assert.assertEquals(Relation.UNKOWN, relationAgnostic);
      
      TestCallTreeStatistics.CONFIG.getStatisticsConfig().setStatisticTest(StatisticalTests.T_TEST);
      Relation relationTTest = StatisticUtil.isDifferent(cd, TestCallTreeStatistics.CONFIG.getStatisticsConfig());
      Assert.assertEquals(Relation.EQUAL, relationTTest);
      
      TestCallTreeStatistics.CONFIG.getStatisticsConfig().setStatisticTest(StatisticalTests.BIMODAL_T_TEST);
      Relation relation = StatisticUtil.isDifferent(cd, TestCallTreeStatistics.CONFIG.getStatisticsConfig());
      Assert.assertEquals(Relation.LESS_THAN, relation);
      
      TestCallTreeStatistics.CONFIG.getStatisticsConfig().setStatisticTest(StatisticalTests.MANN_WHITNEY_TEST);
      Relation relationMannWhitney = StatisticUtil.isDifferent(cd, TestCallTreeStatistics.CONFIG.getStatisticsConfig());
      Assert.assertEquals(Relation.LESS_THAN, relationMannWhitney);
      
      TestCallTreeStatistics.CONFIG.getStatisticsConfig().setStatisticTest(StatisticalTests.CONFIDENCE_INTERVAL);
      Relation relationConfidence = StatisticUtil.isDifferent(cd, TestCallTreeStatistics.CONFIG.getStatisticsConfig());
      Assert.assertEquals(Relation.EQUAL, relationConfidence);
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
