package de.peass.dependencyprocessors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class ProgressWriter implements AutoCloseable {
   private final PrintWriter progressFileWriter;
   private final SummaryStatistics durationStatistics = new SummaryStatistics();
   private final int vms;

   public ProgressWriter(final File resultFile, final int vms) throws FileNotFoundException {
      progressFileWriter = new PrintWriter(resultFile);
      this.vms = vms;
   }

   public void write(final long durationInSeconds, final int finishedVMs) {
      durationStatistics.addValue(durationInSeconds);

      final int duration = (int) ((vms - finishedVMs) * durationStatistics.getMean() / 60);

      int minutes = duration % 60;
      int hours = duration / 60;

      progressFileWriter.write("Finished " + finishedVMs +
            " Duration: " + durationInSeconds +
            " Avg: " + durationStatistics.getMean()
            + " Remaining: " + hours + "h " + minutes + "\n");
      progressFileWriter.flush();
   }

   @Override
   public void close() {
      progressFileWriter.close();
   }
}
