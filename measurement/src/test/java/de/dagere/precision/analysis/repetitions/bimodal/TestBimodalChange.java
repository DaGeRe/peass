package de.dagere.precision.analysis.repetitions.bimodal;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.measurement.statistics.bimodal.BimodalityTester;
import de.dagere.peass.measurement.statistics.bimodal.CompareData;

public class TestBimodalChange {
   
   @Test
   public void testIsBimodalChange() {
      List<VMResult> before = BimodalTestUtil.buildValues(50,100);
      List<VMResult> after = BimodalTestUtil.buildValues(51,101);
      CompareData data = new CompareData(before, after);
      
      final BimodalityTester tester = new BimodalityTester(data);
      Assert.assertTrue(tester.isBimodal());
      Assert.assertTrue(tester.isTChange(0.001));
   }
   
   @Test
   public void testIsBimodalEqual() {
      List<VMResult> before = BimodalTestUtil.buildValues(50,100);
      List<VMResult> after = BimodalTestUtil.buildValues(50,100.5);
      CompareData data = new CompareData(before, after);
      
      final BimodalityTester tester = new BimodalityTester(data);
      Assert.assertTrue(tester.isBimodal());
      Assert.assertFalse(tester.isTChange(0.001));
   }
   
   @Test
   public void testIsUnimodalChange() {
      List<VMResult> before = BimodalTestUtil.buildValues(50,50);
      List<VMResult> after = BimodalTestUtil.buildValues(51,51.5);
      CompareData data = new CompareData(before, after);
      
      final BimodalityTester tester = new BimodalityTester(data);
      Assert.assertFalse(tester.isBimodal());
      Assert.assertTrue(tester.isTChange(0.001));
   }
   
   @Test
   public void testIsUnimodalEqual() {
      List<VMResult> before = BimodalTestUtil.buildValues(50,50);
      List<VMResult> after = BimodalTestUtil.buildValues(50,50.001);
      CompareData data = new CompareData(before, after);
      
      final BimodalityTester tester = new BimodalityTester(data);
      Assert.assertFalse(tester.isBimodal());
      Assert.assertFalse(tester.isTChange(0.001));
   }
   
}
