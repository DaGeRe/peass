package de.dagere.peass.dependency.statistics;

import java.util.LinkedList;
import java.util.List;

import de.dagere.peass.dependency.analysis.data.TestCase;

/**
 * Saves information about count of dependencies, i.e. how many tests are run overall if one considers one dependency/view-file or an evaluation-file.
 * 
 * @author reichelt
 *
 */
public class DependencyStatistics {
   int overallRunTests = 0;
   int changedTraceTests = 0;
   int pruningRunTests = 0;

   int size = 0;

   final List<TestCase> multipleChangedTest = new LinkedList<>();
   final List<TestCase> onceChangedTests = new LinkedList<>();

   public int getOverallRunTests() {
      return overallRunTests;
   }

   public int getChangedTraceTests() {
      return changedTraceTests;
   }

   public int getPruningRunTests() {
      return pruningRunTests;
   }

   public int getSize() {
      return size;
   }

   public List<TestCase> getMultipleChangedTest() {
      return multipleChangedTest;
   }

   public List<TestCase> getOnceChangedTests() {
      return onceChangedTests;
   }
}