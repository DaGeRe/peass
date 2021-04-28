package de.dagere.peass.measurement.analysis.statistics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

import de.dagere.kopeme.generated.Result;
import de.dagere.peass.dependency.analysis.data.TestCase;

/**
 * Represents an pair of measurement results that should be evaluated, i.e. the versions of both measurements and its results.
 * 
 * @author reichelt
 *
 */
public class EvaluationPair {

   private final String previousVersion, currentVersion;
   private final List<Result> previous = new LinkedList<>();
   private final List<Result> current = new LinkedList<>();
   private final TestCase testcase;

   public EvaluationPair(final String currentVersion, final String previousVersion, final TestCase testcase) {
      this.currentVersion = currentVersion;
      this.previousVersion = previousVersion;
      this.testcase = testcase;
      if (currentVersion.equals(previousVersion)) {
         throw new RuntimeException("Unexpected behaviour: Previous " + previousVersion + " == Current " + currentVersion + " version.");
      }
      if (currentVersion == null || previousVersion == null) {
         throw new RuntimeException("Version == null: " + currentVersion + " " + previousVersion + " " + testcase);
      }
   }
   
   public TestCase getTestcase() {
      return testcase;
   }

   public List<Result> getPrevius() {
      return previous;
   }

   public List<Result> getCurrent() {
      return current;
   }

   public void createHistogramFiles(final File currentFile, final File previousFile) {
      printMeans(currentFile, current);
      printMeans(previousFile, previous);
	}

   public static final NumberFormat FORMAT = NumberFormat.getInstance();

   static {
      FORMAT.setGroupingUsed(false);
   }
   
   private void printMeans(final File current, final List<Result> results) {
      try (BufferedWriter writer = new BufferedWriter(new FileWriter(current))){
         for (final Result result : results) {
            final double value = result.getValue();
            writer.write(FORMAT.format(value)+"\n");
         }
         writer.flush();
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   public boolean isComplete() {
      boolean isComplete = previous.size() > 0 && previous.size() == current.size();
      if (isComplete) {
         isComplete &= previous.get(0).getFulldata() != null;
         isComplete &= current.get(0).getFulldata() != null;
         if (isComplete) {
            isComplete &= previous.get(0).getFulldata().getValue().size() > 0;
            isComplete &= current.get(0).getFulldata().getValue().size() > 0;
         }
      }
      return isComplete;
   }

   public String getPreviousVersion() {
      return previousVersion;
   }

   public String getVersion() {
      return currentVersion;
   }
}