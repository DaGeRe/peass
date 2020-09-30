package de.peass.measurement.rca.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.measurement.analysis.statistics.TestcaseStatistic;

/**
 * Saves the call tree structure and measurement data of the call tree
 * 
 * If the measurements are added call by call, the API is:
 * 1) call setVersions with the versions to compare
 * 2) call newVM with the current version
 * 3) call addMeasurement with all values
 * 4) repeat 2) and 3) until all measurements are read
 * 5) call createStatistics with both versions
 * @author reichelt
 *
 */
@JsonDeserialize(using = CallTreeNodeDeserializer.class)
public class CallTreeNode extends BasicNode {

   private static final Logger LOG = LogManager.getLogger(CallTreeNode.class);

   @JsonIgnore
   private final CallTreeNode parent;
   protected final List<CallTreeNode> children = new ArrayList<>();
   protected final Map<String, CallTreeStatistics> data = new HashMap<>();

   protected String version, predecessor;

   private int warmup;

   private CallTreeNode otherVersionNode;

   /**
    * Creates a root node
    */
   public CallTreeNode(final String call, final String kiekerPattern) {
      super(call, kiekerPattern);
      if (!kiekerPattern.contains(call.replace("#", "."))) {
         throw new RuntimeException("Pattern " + kiekerPattern + " must contain " + call + " (with . instead of #)");
      }
      if (kiekerPattern.contains("<init>") && !kiekerPattern.contains("new")) {
         throw new RuntimeException("Pattern " + kiekerPattern + " not legal - Constructor must contain new as return type!");
      }
      this.parent = null;
   }

   protected CallTreeNode(final String call, final String kiekerPattern, final CallTreeNode parent) {
      super(call, kiekerPattern);
      if (!kiekerPattern.contains(call.replace("#", "."))) {
         throw new RuntimeException("Pattern " + kiekerPattern + " must contain " + call);
      }
      if (kiekerPattern.contains("<init>") && !kiekerPattern.contains("new")) {
         throw new RuntimeException("Pattern " + kiekerPattern + " not legal - Constructor must contain new as return type!");
      }
      this.parent = parent;
   }

   public List<CallTreeNode> getChildren() {
      return children;
   }

   public CallTreeNode appendChild(final String call, final String kiekerPattern) {
      final CallTreeNode added = new CallTreeNode(call, kiekerPattern, this);
      children.add(added);
      return added;
   }

   public CallTreeNode getParent() {
      return parent;
   }

   public void addMeasurement(final String version, final Long duration) {
      checkDataAddPossible(version);
      LOG.debug("Adding measurement: {}", version);
      data.get(version).addMeasurement(duration);
   }

   public void setMeasurement(final String version, final List<StatisticalSummary> statistic) {
      checkDataAddPossible(version);
      data.get(version).setMeasurement(statistic);
   }

   private void checkDataAddPossible(final String version) {
      if (otherVersionNode == null) {
         throw new RuntimeException("Other version node needs to be defined before measurement! Node: " + call);
      }
      if (call.equals(CauseSearchData.ADDED) && version.equals(this.predecessor)) {
         throw new RuntimeException("Added methods may not contain data, trying to add data for " + version);
      }
      if (otherVersionNode.getCall().equals(CauseSearchData.ADDED) && version.equals(this.version)) {
         throw new RuntimeException("Added methods may not contain data");
      }
   }

   public boolean hasMeasurement(final String version) {
      return data.get(version).getResults().size() > 0;
   }

   public List<OneVMResult> getResults(final String version) {
      final CallTreeStatistics statistics = data.get(version);
      return statistics != null ? statistics.getResults() : null;
   }

   public void newVM(final String version) {
      final CallTreeStatistics statistics = data.get(version);
      LOG.debug("Adding VM: {} {} VMs: {}", call, version, statistics.getResults().size());
      statistics.newResult();
   }

   private void newVersion(final String version) {
      LOG.trace("Adding version: {}", version);
      CallTreeStatistics statistics = data.get(version);
      if (statistics == null) {
         statistics = new CallTreeStatistics(warmup);
         data.put(version, statistics);
      }
   }

   public void setWarmup(final int warmup) {
      this.warmup = warmup;
   }

   public SummaryStatistics getStatistics(final String version) {
      LOG.debug("Getting data: {}", version);
      final CallTreeStatistics statistics = data.get(version);
      return statistics != null ? statistics.getStatistics() : null;
   }

   public void createStatistics(final String version) {
      LOG.debug("Creating statistics: {}", version);
      final CallTreeStatistics callTreeStatistics = data.get(version);
      callTreeStatistics.createStatistics();
   }

   @Override
   public String toString() {
      return kiekerPattern.toString();
   }

   public ChangedEntity toEntity() {
      if (call.equals(CauseSearchData.ADDED)) {
         return otherVersionNode.toEntity();
      } else {
         final int index = call.lastIndexOf(ChangedEntity.METHOD_SEPARATOR);
         final ChangedEntity entity = new ChangedEntity(call.substring(0, index), "", call.substring(index + 1));
         return entity;
      }
   }

   @JsonIgnore
   public TestcaseStatistic getTestcaseStatistic() {
      final CallTreeStatistics currentVersionStatistics = data.get(version);
      final SummaryStatistics current = currentVersionStatistics.getStatistics();
      final CallTreeStatistics previousVersionStatistics = data.get(predecessor);
      final SummaryStatistics previous = previousVersionStatistics.getStatistics();
      try {
         final TestcaseStatistic testcaseStatistic = new TestcaseStatistic(current, previous, currentVersionStatistics.getCalls(), previousVersionStatistics.getCalls());
         return testcaseStatistic;
      } catch (NumberIsTooSmallException t) {
         LOG.debug("Data: " + current.getN() + " " + previous.getN());
         final String otherCall = otherVersionNode != null ? otherVersionNode.getCall() : "Not Existing";
         throw new RuntimeException("Could not read " + call + " Other Version: " + otherCall, t);
      }
   }

   @JsonIgnore
   public TestcaseStatistic getPartialTestcaseStatistic() {
      final CallTreeStatistics currentVersionStatistics = data.get(version);
      final SummaryStatistics current = currentVersionStatistics.getStatistics();
      final CallTreeStatistics previousVersionStatistics = data.get(predecessor);
      final SummaryStatistics previous = previousVersionStatistics.getStatistics();

      if (firstHasValues(current, previous)) {
         return new TestcaseStatistic(current.getMean(), Double.NaN,
               current.getStandardDeviation(), Double.NaN,
               current.getN(), Double.NaN, true, currentVersionStatistics.getCalls(), 0);
      } else if (firstHasValues(previous, current)) {
         return new TestcaseStatistic(Double.NaN, previous.getMean(),
               Double.NaN, previous.getStandardDeviation(),
               previous.getN(), Double.NaN, true, 0, previousVersionStatistics.getCalls());
      } else if ((current == null || current.getN() == 0) && (previous == null || previous.getN() == 0)) {
         LOG.error("Could not measure {}", this);
         return new TestcaseStatistic(Double.NaN, Double.NaN,
               Double.NaN, Double.NaN,
               0, Double.NaN, false, 0, 0);
      } else {
         throw new RuntimeException("Partial statistics should exactly be created if one node is unmeasurable");
      }
   }

   private boolean firstHasValues(final SummaryStatistics first, final SummaryStatistics second) {
      return (second == null || second.getN() == 0) && (first != null && first.getN() > 0);
   }

   @JsonIgnore
   public void setVersions(final String version, final String predecessor) {
      this.version = version;
      this.predecessor = predecessor;
      resetStatistics();
      newVersion(version);
      newVersion(predecessor);
   }

   @JsonIgnore
   public int getTreeSize() {
      int size = 1;
      for (final CallTreeNode child : children) {
         size += child.getTreeSize();
      }
      return size;
   }

   protected void resetStatistics() {
      data.values().forEach(statistics -> statistics.resetResults());
   }

   @JsonIgnore
   public CallTreeNode getOtherVersionNode() {
      return otherVersionNode;
   }

   public void setOtherVersionNode(final CallTreeNode otherVersionNode) {
      this.otherVersionNode = otherVersionNode;
   }

   @JsonIgnore
   public String getMethod() {
      final String method = call.substring(call.lastIndexOf('#'));
      return method;
   }

   @JsonIgnore
   public String getParameters() {
      final String parameters = kiekerPattern.substring(kiekerPattern.indexOf('('));
      return parameters;
   }

   @JsonIgnore
   public int getEss() {
      return parent != null ? parent.getEss() + 1 : 0;
   }

   @JsonIgnore
   public int getEoi() {
      int eoi;
      if (parent != null) {
         int predecessorIndex = parent.getChildren().indexOf(this) - 1;
         if (predecessorIndex >= 0) {
            CallTreeNode predecessor = parent.getChildren().get(predecessorIndex);
            eoi = predecessor.getEoi() + predecessor.getAllChildCount() + 1;
         } else {
            eoi = parent.getEoi() + 1;
         }
      } else {
         eoi = 0;
      }
      return eoi;
   }

   private int getAllChildCount() {
      int childs = 0;
      for (CallTreeNode child : children) {
         childs += child.getAllChildCount() + 1;
      }
      return childs;
   }

   @JsonIgnore
   public int getPosition() {
      if (parent != null) {
         for (int childIndex = 0; childIndex < parent.getChildren().size(); childIndex++) {
            if (parent.getChildren().get(childIndex) == this) {
               return childIndex;
            }
         }
         return -1;
      } else {
         return 0;
      }
   }

   public long getCallCount(final String version) {
      return data.get(version).getResults().stream().mapToLong(result -> result.getCalls()).sum();
   }

   @Override
   public int hashCode() {
      return kiekerPattern.hashCode();
   }

   @Override
   public boolean equals(final Object obj) {
      if (obj instanceof CallTreeNode) {
         final CallTreeNode other = (CallTreeNode) obj;
         boolean equal = other.getKiekerPattern().equals(kiekerPattern);
         if (equal) {
            if ((this.parent == null) != (other.parent == null)) {
               equal = false;
            } else if (parent != null) {
               equal &= this.parent.equals(other.parent);
               equal &= (this.getPosition() == other.getPosition());
            }
         }
         return equal;
      } else {
         return false;
      }
   }

}