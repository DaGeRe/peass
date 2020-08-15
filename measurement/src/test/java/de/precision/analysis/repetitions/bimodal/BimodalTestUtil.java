package de.precision.analysis.repetitions.bimodal;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import de.dagere.kopeme.generated.Result;

public class BimodalTestUtil {
   
   private static final Random RANDOM = new Random();
   
   public static List<Result> buildValues(double first, double second) {
      List<Result> before = new LinkedList<>();
      addValues(before, first);
      addValues(before, second);
      return before;
   }

   private static void addValues(List<Result> before, final double mean) {
      for (int i = 0; i < 100; i++) {
         Result r = new Result();
         r.setValue(mean + RANDOM.nextDouble());
         before.add(r);
      }
   }
}
