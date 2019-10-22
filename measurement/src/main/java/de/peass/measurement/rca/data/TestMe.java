package de.peass.measurement.rca.data;

import java.util.Arrays;

import kieker.common.record.controlflow.OperationExecutionRecord;

public class TestMe {
   private static final int COUNT = 100000;

   public static void main(final String[] args) {
      for (int i = 0; i < 10000; i++) {
         final OperationExecutionRecord[] diffs = new OperationExecutionRecord[100000];
         for (int j = 0; j < COUNT; j++) {
            
            final long time1 = System.nanoTime();
            final long time2 = System.nanoTime();
            final OperationExecutionRecord record = new OperationExecutionRecord("test", "1", 15, time1, time2, "asd", 1, 2);
            diffs[j] = record;
         }
         final double avg = Arrays.asList(diffs).stream().mapToLong(val -> val.getTout() - val.getTin()).average().getAsDouble();
         System.out.println(avg);
      }
   }
}
