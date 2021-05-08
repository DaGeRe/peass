
package de.dagere.peass;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Scope;

@State(Scope.Benchmark)
public class ExampleBenchmark {

   @Param({"val1", "val2"})
   public static String parameter;

   @Benchmark
   public void testMethod() {
      try {
         Thread.sleep(1);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }
}
