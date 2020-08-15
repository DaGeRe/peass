package de.precision.analysis.repetitions.bimodal;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.dagere.kopeme.generated.Result;

public class TestOutlierRemoval {
   
   @Test
   public void testNoRemovalUnimodal() {
      List<Result> basicValues = BimodalTestUtil.buildValues(50, 50);
      new OutlierRemoverBimodal(basicValues);
      Assert.assertEquals(200, basicValues.size());
      
      addValue(basicValues, 50.5);
      new OutlierRemoverBimodal(basicValues);
      Assert.assertEquals(201, basicValues.size());
   }

   

   @Test
   public void testRemovalUnimodal() {
      List<Result> basicValues = BimodalTestUtil.buildValues(50, 50);
      addValue(basicValues, 58);
      new OutlierRemoverBimodal(basicValues);
      Assert.assertEquals(200, basicValues.size());
   }
   
   
   @Test
   public void testNoRemovalBimodal() {
      List<Result> basicValues = BimodalTestUtil.buildValues(50, 100);
      new OutlierRemoverBimodal(basicValues);
      Assert.assertEquals(200, basicValues.size());

      addValue(basicValues, 50.5);
      addValue(basicValues, 100.5);
      new OutlierRemoverBimodal(basicValues);
      Assert.assertEquals(202, basicValues.size());
   }

   @Test
   public void testRemovalBimodal() {
      List<Result> basicValues = BimodalTestUtil.buildValues(50, 100);
      addValue(basicValues, 58);
      addValue(basicValues, 108);
      addValue(basicValues, 42);
      addValue(basicValues, 92);
      new OutlierRemoverBimodal(basicValues);
      Assert.assertEquals(200, basicValues.size()); 
   }
   
   private void addValue(List<Result> basicValues, final double value) {
      final Result result2 = new Result();
      result2.setValue(value);
      basicValues.add(result2);
   }
}
