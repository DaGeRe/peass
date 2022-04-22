package de.dagere.precision.analysis.repetitions.bimodal;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.measurement.statistics.bimodal.OutlierRemoverBimodal;

public class TestOutlierRemoval {
   
   @Test
   public void testNoRemovalUnimodal() {
      List<VMResult> basicValues = BimodalTestUtil.buildValues(50, 50);
      new OutlierRemoverBimodal(basicValues);
      Assert.assertEquals(200, basicValues.size());
      
      addValue(basicValues, 50.01);
      new OutlierRemoverBimodal(basicValues);
      Assert.assertEquals(201, basicValues.size());
   }

   

   @Test
   public void testRemovalUnimodal() {
      List<VMResult> basicValues = BimodalTestUtil.buildValues(50, 50);
      addValue(basicValues, 58);
      new OutlierRemoverBimodal(basicValues);
      Assert.assertEquals(200, basicValues.size());
   }
   
   
   @Test
   public void testNoRemovalBimodal() {
      List<VMResult> basicValues = BimodalTestUtil.buildValues(50, 100);
      new OutlierRemoverBimodal(basicValues);
      Assert.assertEquals(200, basicValues.size());

      addValue(basicValues, 50.01);
      addValue(basicValues, 100.01);
      new OutlierRemoverBimodal(basicValues);
      Assert.assertEquals(202, basicValues.size());
   }

   @Test
   public void testRemovalBimodal() {
      List<VMResult> basicValues = BimodalTestUtil.buildValues(50, 100);
      addValue(basicValues, 58);
      addValue(basicValues, 108);
      addValue(basicValues, 42);
      addValue(basicValues, 92);
      new OutlierRemoverBimodal(basicValues);
      Assert.assertEquals(200, basicValues.size()); 
      
   }
   
   private void addValue(final List<VMResult> basicValues, final double value) {
      final VMResult result2 = new VMResult();
      result2.setValue(value);
      basicValues.add(result2);
   }
}
