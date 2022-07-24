package de.dagere.peass.analysis.measurement;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependencyprocessors.CommitByNameComparator;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;

public class ProjectStatistics {

   private static final Logger LOG = LogManager.getLogger(ProjectStatistics.class);

   public Map<String, Map<TestCase, TestcaseStatistic>> statistics = VersionComparator.hasVersions() ? new TreeMap<>(VersionComparator.INSTANCE) : new LinkedHashMap<>();

   public ProjectStatistics() {
      statistics = VersionComparator.hasVersions() ? new TreeMap<>(CommitByNameComparator.INSTANCE) : new LinkedHashMap<>();
   }
   
   public ProjectStatistics(CommitComparatorInstance comparator) {
      statistics = new TreeMap<>(comparator);
   }
   
   public Map<String, Map<TestCase, TestcaseStatistic>> getStatistics() {
      return statistics;
   }

   public void setStatistics(final Map<String, Map<TestCase, TestcaseStatistic>> statistics) {
      this.statistics = statistics;
   }

   public void addMeasurement(final String commit, final TestCase test, final DescriptiveStatistics statisticsOld, final DescriptiveStatistics statisticsCurrent, final int calls) {
      final TestcaseStatistic statistic = new TestcaseStatistic(statisticsOld, statisticsCurrent, calls, calls);
      addMeasurement(commit, test, statistic);
   }

   public void addMeasurement(final String commit, final TestCase test, final TestcaseStatistic statistic) {
      Map<TestCase, TestcaseStatistic> commitMap = statistics.get(commit);
      if (commitMap == null) {
         commitMap = new TreeMap<>();
         statistics.put(commit, commitMap);
      }
      if (commitMap.containsKey(test)) {
         LOG.error("Test " + test + " already executed in " + commit + " - two measurements!");
      }
      commitMap.put(test, statistic);

   }

   @JsonIgnore
   public int getTestCount() {
      int tests = 0;
      for (Map<TestCase, TestcaseStatistic> commitStatistic : statistics.values()) {
         tests += commitStatistic.size();
      }
      return tests;
   }
}
