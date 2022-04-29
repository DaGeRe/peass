package de.dagere.peass.analysis.measurement.statistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import de.dagere.kopeme.kopemedata.VMResult;


public class MeanHistogramData {
   private final List<VMResult> values;

   public MeanHistogramData(List<VMResult> values) {
      this.values = values;
   }

   public void printHistData(File file) {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
         for (final VMResult result : values) {
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
      for (final VMResult r : values) {
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
