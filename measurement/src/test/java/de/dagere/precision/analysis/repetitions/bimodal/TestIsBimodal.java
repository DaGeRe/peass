package de.dagere.precision.analysis.repetitions.bimodal;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Assert;
import org.junit.Test;

import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.measurement.statistics.bimodal.IsBimodal;

public class TestIsBimodal {
   
   SummaryStatistics stat;
   double[] data;
   
   @Test
   public void testIsNotBimodal() {
      buildData(50, 50);
      
      Assert.assertFalse(new IsBimodal(data, stat).isBimodal());
   }
   
   @Test
   public void testIsBimodal() {
      buildData(50, 100);
      
      Assert.assertTrue(new IsBimodal(data, stat).isBimodal());
   }
   
   @Test
   public void testIsBimodalOneSmallerLeft() {
      stat = new SummaryStatistics();
      data = new double[10];
      
      data[0] = 7;
      data[1] = 8;
      data[2] = 7.5;
      data[3] = 10;
      data[4] = 11;
      data[5] = 10.5;
      data[6] = 10;
      data[7] = 12.5;
      data[8] = 11.5;
      data[9] = 10;
      
      for (double value : data) {
         stat.addValue(value);
      }
      
      IsBimodal isBimodal = new IsBimodal(data, stat);
      Assert.assertTrue(isBimodal.isBimodal());
   }
   
   @Test
   public void testIsBimodalOneSmallerRight() {
      stat = new SummaryStatistics();
      data = new double[10];
      
      data[0] = 7;
      data[1] = 8;
      data[2] = 7.5;
      data[3] = 8; 
      data[4] = 7.2;
      data[5] = 7;
      data[6] = 7;
      data[7] = 12;
      data[8] = 11.5;
      data[9] = 10.5;
      
      for (double value : data) {
         stat.addValue(value);
      }
      
      IsBimodal isBimodal = new IsBimodal(data, stat);
      Assert.assertTrue(isBimodal.isBimodal());
   }
   
   @Test
   public void testIsBimodalNearlyReal() {
      stat = new SummaryStatistics();
      data = new double[10];
      
      data[0] = 3.7;
      data[1] = 3.6;
      data[2] = 3.8;
      data[3] = 3.7;
      data[4] = 4.1;
      data[5] = 4.0;
      data[6] = 4.2;
      data[7] = 4.0;
      data[8] = 4.1;
      data[9] = 4.3;
      
      for (double value : data) {
         stat.addValue(value);
      }
      System.out.println(stat.getMean() + " " + stat.getVariance());
      
      IsBimodal isBimodal = new IsBimodal(data, stat);
      Assert.assertTrue(isBimodal.isBimodal());
   }

   private void buildData(final double val, final double val2) {
      List<VMResult> before = BimodalTestUtil.buildValues(val,val2);
      
      stat = new SummaryStatistics();
      data = new double[before.size()];
      int index = 0;
      for (VMResult result : before) {
         data[index] = result.getValue();
         stat.addValue(data[index]);
         index++;
      }
   }
}
