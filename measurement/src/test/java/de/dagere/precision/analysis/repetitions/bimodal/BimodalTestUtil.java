package de.dagere.precision.analysis.repetitions.bimodal;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import de.dagere.kopeme.kopemedata.VMResult;

/**
 * Provides demo data for testing bimodality; by calling the main method, the diff[] data can be regenerated (e.g. if new size is needed).
 * @author reichelt
 *
 */
public class BimodalTestUtil {

   private static double[] diffs = new double[] { 0.008236, 0.006328, 0.008502, 0.006951000000000001, 0.005308, 0.006927, 0.00884, 0.001687, 0.0027800000000000004, 0.005181, 2.3E-5,
         0.007893, 0.002431, 0.007397, 0.006965, 0.003327, 0.0017699999999999999, 0.00329, 0.005723000000000001, 0.0026479999999999997, 0.005395, 0.007929, 0.003227, 0.008079,
         0.008394, 0.007641, 0.007733, 0.002366, 0.0032440000000000004, 0.007723, 0.0070420000000000005, 0.009414, 0.005725, 0.008337, 0.007587, 0.009724, 0.005926, 0.002506,
         0.005104, 0.003574, 0.007606000000000001, 0.0067020000000000005, 0.006823, 0.004817, 0.0028870000000000002, 2.62E-4, 0.005842000000000001, 0.002085, 0.009531, 0.004531,
         6.66E-4, 0.002231, 0.0049, 0.006477, 0.006777999999999999, 0.0029049999999999996, 0.004122, 0.004372, 0.0025340000000000002, 0.007323, 0.009936, 0.005044, 0.008707,
         0.004462, 0.008237, 0.003215, 0.009516, 0.0022140000000000003, 0.006419, 0.003463, 0.003166, 0.009345, 0.0013089999999999998, 0.004477999999999999, 0.0015559999999999999,
         0.004494000000000001, 0.009657, 0.003454, 0.0021190000000000002, 0.0069099999999999995, 0.0069689999999999995, 0.005667, 0.009513, 0.004155, 0.009800999999999999, 6.39E-4,
         0.001321, 0.006541, 0.00253, 0.001598, 0.009804, 0.006762000000000001, 0.002121, 0.009399999999999999, 0.0012900000000000001, 0.005848, 0.003396, 0.005560000000000001,
         0.005825, 0.003357, };

   

   public static List<VMResult> buildValues(final double first, final double second) {
      List<VMResult> before = new LinkedList<>();
      addValues(before, first);
      addValues(before, second);
      return before;
   }

   private static void addValues(final List<VMResult> before, final double mean) {
      for (int i = 0; i < 100; i++) {
         VMResult r = new VMResult();
         r.setValue(mean + diffs[i]);
         before.add(r);
      }
   }
   
   private static final Random RANDOM = new Random();

   public static void main(final String[] args) {
      System.out.print("diffs=new double[]{");
      for (int i = 0; i < 100; i++) {
         double rounded = Math.round(10000 * RANDOM.nextDouble()) / 10000d;
         System.out.print(rounded / 100 + ",");
      }
      System.out.println("};");
   }
}
