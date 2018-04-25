package de.peran.measurement.analysis;

import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.inference.TestUtils;

public class MyTest {
   private static final double CONFIDENCE = 0.01;

   public static void main(String[] args) {
      final double[] vals1 = new double[] { 76, 82, 74, 156, 76 };
      final double[] vals2 = new double[] { 211, 205, 204, 200, 293 };

      final boolean change = TestUtils.tTest(vals1, vals2, CONFIDENCE);

      final boolean change2 = new TTest().homoscedasticTTest(vals1, vals2, 0.01);

      System.out.println(change);
      System.out.println(change2);
   }
}
