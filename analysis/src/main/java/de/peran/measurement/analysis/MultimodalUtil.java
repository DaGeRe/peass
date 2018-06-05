package de.peran.measurement.analysis;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;

import de.dagere.kopeme.generated.TestcaseType.Datacollector.Result;

public class MultimodalUtil {
   public static boolean isMultimodalCoefficient(List<Result> list) {
      final SummaryStatistics statistics1 = MultipleVMTestUtil.getStatistic(list);
      
      final DescriptiveStatistics st = new DescriptiveStatistics();
      final double[] values = new double[list.size()];
      int i = 0;
      for (final Result r : list) {
         values[i] = r.getValue();
         i++;
      }
      final double kurtosis = new Kurtosis().evaluate(values);
      final double skewness = new Skewness().evaluate(values);
      
      System.out.println(Arrays.toString(values));
      final int n = values.length;
      System.out.println(skewness + " " + kurtosis);
      final double factor = 3*Math.pow(n-1, 2) / ((n-2)*(n-3));
      final double bimodalityCoefficient = (Math.pow(skewness, 2)+1) / (kurtosis + factor);
      final boolean isMultimodal = bimodalityCoefficient > (5d/9);
      System.out.println("Coefficien: " + bimodalityCoefficient + " " + isMultimodal);
      
      return isMultimodal;
   }
}
