package de.peass.measurement.searchcause.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

public class CallTreeStatistics {
   private final List<CallTreeChunk> chunks = new ArrayList<>();
   private final DescriptiveStatistics statistics = new DescriptiveStatistics();

   public void addMeasurement(final Long duration) {
      CallTreeChunk current = chunks.get(chunks.size() - 1);
      current.addValue(duration);
   }

   public void newChunk() {
      chunks.add(new CallTreeChunk());
   }

   public void createStatistics(final int warmup) {
      statistics.clear();
      for (CallTreeChunk chunk : chunks) {
         final List<Long> values = chunk.values.subList(warmup, chunk.values.size());
         final double average = values.stream().mapToLong(val -> val).average().getAsDouble();
         statistics.addValue(average);
      }
   }

   public DescriptiveStatistics getStatistics() {
      return statistics;
   }
}