package de.dagere.peass.analysis.changes;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.dagere.nodeDiffDetector.data.TestCase;
import de.dagere.nodeDiffDetector.data.TestMethodCall;
import de.dagere.peass.config.StatisticsConfig;
import de.dagere.peass.dependencyprocessors.CommitComparatorInstance;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.measurement.statistics.Relation;

/**
 * Saves knowledge about changes. Truly a statistics file. This should be in analysis.
 * 
 * @author reichelt
 *
 */
public class ProjectChanges implements Serializable {

   private static final long serialVersionUID = 5377574657253392155L;

   private static final Logger LOG = LogManager.getLogger(ProjectChanges.class);

   // Metadata for analysis
   private int commitCount;
   private int changeCount;
   private int testcaseCount;
   private StatisticsConfig statisticsConfig;
   private Map<String, Changes> commitChanges = VersionComparator.hasVersions() ? new TreeMap<>(VersionComparator.INSTANCE) : new LinkedHashMap<>();

   public ProjectChanges() {
   }
   
   public ProjectChanges(CommitComparatorInstance comparator) {
      commitChanges = new TreeMap<>(comparator);
   }


   public ProjectChanges(final StatisticsConfig statisticsConfig, CommitComparatorInstance comparator) {
      this(comparator);
      this.statisticsConfig = statisticsConfig;
   }

   public StatisticsConfig getStatisticsConfig() {
      return statisticsConfig;
   }

   public void setStatisticsConfig(final StatisticsConfig statisticsConfig) {
      this.statisticsConfig = statisticsConfig;
   }

   public int getCommitCount() {
      return commitCount;
   }

   public void setCommitCount(final int commits) {
      this.commitCount = commits;
   }

   public int getChangeCount() {
      return changeCount;
   }

   public void setChangeCount(final int changes) {
      this.changeCount = changes;
   }

   public int getTestcaseCount() {
      return testcaseCount;
   }

   public void setTestcaseCount(final int testcases) {
      this.testcaseCount = testcases;
   }

   public Map<String, Changes> getCommitChanges() {
      return commitChanges;
   }

   public void setCommitChanges(final Map<String, Changes> commitChanges) {
      this.commitChanges = commitChanges;
   }

   public void addChange(final TestMethodCall testCase, final String commit,
         final Relation confidenceResult,
         final Relation tTestResult, final double oldTime,
         final double diffPercent, 
         final double tvalue, double mannWhitheyUStatistic,
         final long vms) {
      final Changes changeList = getCommitChanges(commit);
      final String viewName = "view_" + commit + "/diffs/" + testCase.getShortClazz() + "#" + testCase.getMethod() + ".txt";
      LOG.debug("Adding change: {} to {}", testCase, commit);
      changeList.addChange(testCase, viewName, oldTime, diffPercent, tvalue, mannWhitheyUStatistic, vms);

      changeCount++;
   }

   public void addChange(final TestCase test, final String commit, final Change change) {
      final Changes changeList = getCommitChanges(commit);
      final String clazz = test.getTestclazzWithModuleName();
      changeList.addChange(clazz, change);
      changeCount++;

   }

   @JsonIgnore
   public Changes getCommitChanges(final String key) {
      Changes result = commitChanges.get(key);
      if (result == null) {
         result = new Changes();
         commitChanges.put(key, result);
      }
      return result;
   }

   public void executeProcessor(final ChangeProcessor c) {
      for (final Map.Entry<String, Changes> commit : commitChanges.entrySet()) {
         for (final Entry<String, List<Change>> testcase : commit.getValue().getTestcaseChanges().entrySet()) {
            for (final Change change : testcase.getValue()) {
               c.process(commit.getKey(), testcase.getKey(), change);
            }
         }
      }
   }
}
