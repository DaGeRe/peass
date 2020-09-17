package de.peass.measurement.rca.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.RuntimeErrorException;

import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.measurement.analysis.statistics.TestcaseStatistic;

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
         throw new RuntimeException("Pattern " + kiekerPattern + " must contain " + call);
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
      data.get(version).addMeasurement(duration);
   }

   public void setMeasurement(final String version, final List<StatisticalSummary> statistic) {
      data.get(version).setMeasurement(statistic);
   }

   public boolean hasMeasurement(final String version) {
      return data.get(version).getResults().size() > 0;
   }

   public List<OneVMResult> getResults(final String version) {
      return data.get(version).getResults();
   }

   public void newVM(final String version) {
      LOG.debug("Adding VM: {}", version);
      final CallTreeStatistics statistics = data.get(version);
      LOG.debug("VMs: {}", statistics.getResults().size());
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
      if (statistics.getStatistics().getN() == 0) {
         LOG.error("Call createStatistics first for " + call);
      }
      return statistics.getStatistics();
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
      final int index = call.lastIndexOf(ChangedEntity.METHOD_SEPARATOR);
      final ChangedEntity entity = new ChangedEntity(call.substring(0, index), "", call.substring(index + 1));
      return entity;
   }

   @JsonIgnore
   public TestcaseStatistic getTestcaseStatistic() {
      final SummaryStatistics current = data.get(version).getStatistics();
      final SummaryStatistics previous = data.get(predecessor).getStatistics();
      try {
         final TestcaseStatistic testcaseStatistic = new TestcaseStatistic(current, previous, data.get(version).getCalls(), data.get(predecessor).getCalls());
         return testcaseStatistic;
      } catch (NumberIsTooSmallException t) {
         LOG.debug("Data: " + current.getN() + " " + previous.getN());
         final String otherCall = otherVersionNode != null ? otherVersionNode.getCall() : "Not Existing";
         throw new RuntimeException("Could not read " + call + " Other Version: " + otherCall, t);
      }
      
   }

   @JsonIgnore
   public TestcaseStatistic getPartialTestcaseStatistic() {
      final SummaryStatistics current = data.get(version).getStatistics();
      final SummaryStatistics previous = data.get(predecessor).getStatistics();
      if (current.getN() > 0 && previous.getN() == 0) {
         return new TestcaseStatistic(current.getMean(), Double.NaN, 
               current.getStandardDeviation(), Double.NaN, 
               current.getN(), Double.NaN, true);
      } else if (previous.getN() > 0) {
         return new TestcaseStatistic(Double.NaN, previous.getMean(), 
               Double.NaN, previous.getStandardDeviation(), 
               current.getN(), Double.NaN, true);
      } else {
         throw new RuntimeException("Partial statistics should only be created if one node is unmeasurable");
      }
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