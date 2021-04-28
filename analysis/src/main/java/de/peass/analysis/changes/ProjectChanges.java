package de.peass.analysis.changes;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.dagere.peass.analysis.changes.Change;
import de.dagere.peass.dependency.analysis.data.TestCase;
import de.dagere.peass.dependencyprocessors.VersionComparator;
import de.dagere.peass.measurement.analysis.Relation;
import de.peass.analysis.changes.processors.ChangeProcessor;

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
   private Map<String, Changes> versionChanges = VersionComparator.hasDependencies() ? new TreeMap<>(VersionComparator.INSTANCE) : new LinkedHashMap<>();

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

   public Map<String, Changes> getVersionChanges() {
      return versionChanges;
   }

   public void setVersionChanges(final Map<String, Changes> versionChanges) {
      this.versionChanges = versionChanges;
   }

   public void addChange(final TestCase testCase, final String versionTag,
         final Relation confidenceResult,
         final Relation tTestResult, final double oldTime,
         final double diffPercent, final double tvalue,
         final long vms) {
      final Changes changeList = getVersion(versionTag);
      final String viewName = "view_" + versionTag + "/diffs/" + testCase.getShortClazz() + "#" + testCase.getMethod() + ".txt";
      LOG.trace("Adding change: " + testCase);
      changeList.addChange(testCase, viewName, oldTime, diffPercent, tvalue, vms);

      changeCount++;
   }

   public void addChange(final TestCase test, final String versionTag, final Change change) {
      final Changes changeList = getVersion(versionTag);
      final String clazz = test.getTestclazzWithModuleName();
      changeList.addChange(clazz, change);
      changeCount++;

   }

   @JsonIgnore
   public Changes getVersion(final String key) {
      Changes result = versionChanges.get(key);
      if (result == null) {
         result = new Changes();
         versionChanges.put(key, result);
      }
      return result;
   }

   public void executeProcessor(final ChangeProcessor c) {
      for (final Map.Entry<String, Changes> version : versionChanges.entrySet()) {
         for (final Entry<String, List<Change>> testcase : version.getValue().getTestcaseChanges().entrySet()) {
            for (final Change change : testcase.getValue()) {
               c.process(version.getKey(), testcase.getKey(), change);
            }
         }
      }
   }
}
