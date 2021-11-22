
package de.dagere.peass;

import org.openjdk.jmh.annotations.Benchmark;

public class ExampleBenchmark {

   @Benchmark
   public void testMethod() {
      try {
         Thread.sleep(50);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }
}
