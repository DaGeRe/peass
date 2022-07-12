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

import de.dagere.peass.analysis.changes.processors.ChangeProcessor;
import de.dagere.peass.config.StatisticsConfig;
import de.dagere.peass.dependency.analysis.data.TestCase;
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
   private int versionCount;
   private int changeCount;
   private int testcaseCount;
   private StatisticsConfig statisticsConfig;
   private Map<String, Changes> commitChanges = VersionComparator.hasVersions() ? new TreeMap<>(VersionComparator.INSTANCE) : new LinkedHashMap<>();

   public ProjectChanges() {
   }

   public ProjectChanges(final StatisticsConfig statisticsConfig) {
      this.statisticsConfig = statisticsConfig;
   }

   public StatisticsConfig getStatisticsConfig() {
      return statisticsConfig;
   }

   public void setStatisticsConfig(final StatisticsConfig statisticsConfig) {
      this.statisticsConfig = statisticsConfig;
   }

   public int getVersionCount() {
      return versionCount;
   }

   public void setVersionCount(final int versions) {
      this.versionCount = versions;
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

   public void addChange(final TestCase testCase, final String commit,
         final Relation confidenceResult,
         final Relation tTestResult, final double oldTime,
         final double diffPercent, final double tvalue,
         final long vms) {
      final Changes changeList = getCommitChanges(commit);
      final String viewName = "view_" + commit + "/diffs/" + testCase.getShortClazz() + "#" + testCase.getMethod() + ".txt";
      LOG.trace("Adding change: " + testCase);
      changeList.addChange(testCase, viewName, oldTime, diffPercent, tvalue, vms);

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
      for (final Map.Entry<String, Changes> version : commitChanges.entrySet()) {
         for (final Entry<String, List<Change>> testcase : version.getValue().getTestcaseChanges().entrySet()) {
            for (final Change change : testcase.getValue()) {
               c.process(version.getKey(), testcase.getKey(), change);
            }
         }
      }
   }
}
