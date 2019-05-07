package de.peran.measurement.analysis.statistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import de.dagere.kopeme.generated.Result;


public class MeanHistogramData {
   private final List<Result> values;

   public MeanHistogramData(List<Result> values) {
      this.values = values;
   }

   public void printHistData(File file) {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
         for (final Result result : values) {
            final double value = result.getValue();
            writer.write(MeanCoVData.FORMAT.format(value) + "\n");
         }
         writer.flush();
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }
   
   public double getSpan() {
      double min = Double.MAX_VALUE;
      double max = Double.MIN_VALUE;
      for (final Result r : values) {
         if (r.getValue() < min) {
            min = r.getValue();
         }
         if (r.getValue() > max) {
            max = r.getValue();
         }
      }
      return max - min;
   }
}
