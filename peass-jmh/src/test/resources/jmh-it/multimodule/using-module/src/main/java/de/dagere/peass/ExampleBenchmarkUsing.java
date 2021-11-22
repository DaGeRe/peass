
package de.dagere.peass;

import org.openjdk.jmh.annotations.Benchmark;

public class ExampleBenchmarkUsing {

   @Benchmark
   public void testMethod() {
      try {
         Thread.sleep(1);
      } catch (InterruptedException e) {
         e.printStackTrace();
      }
   }
}
