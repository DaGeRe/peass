package de.precision.analysis.repetitions.bimodal;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import de.dagere.kopeme.generated.Result;
import de.precision.analysis.repetitions.bimodal.BimodalityTester;

public class TestBimodalChange {
   
   @Test
   public void testIsBimodalChange() {
      List<Result> before = BimodalTestUtil.buildValues(50,100);
      List<Result> after = BimodalTestUtil.buildValues(51,101);
      CompareData data = new CompareData(before, after);
      
      final BimodalityTester tester = new BimodalityTester(data);
      Assert.assertTrue(tester.isBimodal());
      Assert.assertTrue(tester.isTChange(0.001));
   }
   
   @Test
   public void testIsBimodalEqual() {
      List<Result> before = BimodalTestUtil.buildValues(50,100);
      List<Result> after = BimodalTestUtil.buildValues(50,100.5);
      CompareData data = new CompareData(before, after);
      
      final BimodalityTester tester = new BimodalityTester(data);
      Assert.assertTrue(tester.isBimodal());
      Assert.assertFalse(tester.isTChange(0.001));
   }
   
   @Test
   public void testIsUnimodalChange() {
      List<Result> before = BimodalTestUtil.buildValues(50,50);
      List<Result> after = BimodalTestUtil.buildValues(51,51.5);
      CompareData data = new CompareData(before, after);
      
      final BimodalityTester tester = new BimodalityTester(data);
      Assert.assertFalse(tester.isBimodal());
      Assert.assertTrue(tester.isTChange(0.001));
   }
   
   @Test
   public void testIsUnimodalEqual() {
      List<Result> before = BimodalTestUtil.buildValues(50,50);
      List<Result> after = BimodalTestUtil.buildValues(50,50.1);
      CompareData data = new CompareData(before, after);
      
      final BimodalityTester tester = new BimodalityTester(data);
      Assert.assertFalse(tester.isBimodal());
      Assert.assertFalse(tester.isTChange(0.001));
   }
   
}
