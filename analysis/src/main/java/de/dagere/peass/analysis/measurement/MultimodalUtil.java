package de.dagere.peass.analysis.measurement;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;

import de.dagere.kopeme.kopemedata.VMResult;
import de.dagere.peass.utils.StreamGobbler;

public class MultimodalUtil {
   public static boolean isMultimodalCoefficient(final List<VMResult> list) {
      final double[] values = new double[list.size()];
      int i = 0;
      for (final VMResult r : list) {
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
   
   public static boolean isRInstalled() {
      try {
         return Runtime.getRuntime().exec("which R").waitFor() == 0;
      } catch (InterruptedException | IOException e) {
         e.printStackTrace();
      }
      return false;
   }
   
   public static boolean isMultimodalSilverman(final List<VMResult> list) {
      final double pvalue = getPValue(list);
      return pvalue > 0.5;
   }

   private static double getPValue(final List<VMResult> list) {
      String arrayString = "";
      for (final VMResult r : list) {
         arrayString += r.getValue()+",";
      }
      final String command = "dist=c("+arrayString.substring(0,arrayString.length()-1)+"); require(silvermantest); silverman.test(dist,1)";
      //System.out.println(command);
      try {
         final String call = "R -e \"" + command + "\"";
         System.out.println(call);
         final Process process =new ProcessBuilder("R", "-e", command).start();
         final String result = StreamGobbler.getFullProcess(process, false);
         for (final String line : result.split("\n")) {
            if (line.startsWith("The P-Value is")) {
               final double pvalue = Double.parseDouble(line.replace("The P-Value is","").replaceAll(" ", ""));
               return pvalue;
            }
         }
      } catch (final IOException e) {
         e.printStackTrace();
      }
      return 0.0;
   }
}
