package de.precision.analysis.repetitions.bimodal;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.Assert;
import org.junit.Test;

import de.dagere.kopeme.generated.Result;

public class TestIsBimodal {
   
   SummaryStatistics stat;
   double[] data;
   
   @Test
   public void testIsNotBimodal() {
      buildData(50, 50);
      
      Assert.assertFalse(new IsBimodal(data, stat.getMean(), stat).isBimodal());
   }
   
   @Test
   public void testIsBimodal() {
      buildData(50, 100);
      
      Assert.assertTrue(new IsBimodal(data, stat.getMean(), stat).isBimodal());
   }

   private void buildData(final double val, final double val2) {
      List<Result> before = BimodalTestUtil.buildValues(val,val2);
      
      stat = new SummaryStatistics();
      data = new double[before.size()];
      int index = 0;
      for (Result result : before) {
         data[index] = result.getValue();
         stat.addValue(data[index]);
         index++;
      }
   }
}
