package de.dagere.peass.measurement.rca.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.math3.exception.NumberIsTooSmallException;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.dagere.nodeDiffDetector.data.MethodCall;
import de.dagere.peass.config.FixedCommitConfig;
import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.statistics.StatisticUtil;
import de.dagere.peass.measurement.statistics.bimodal.CompareData;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic;
import de.dagere.peass.measurement.statistics.data.TestcaseStatistic.TestcaseStatisticException;

/**
 * Saves the call tree structure and measurement data of the call tree
 * 
 * If the measurements are added call by call, the API is:
 * 
 * 1) call initCommitData (the MeasurementConfig should contain the commits to compare)
 * 
 * 2) call initVMData with the current commit
 * 
 * 3) call addMeasurement with all values
 * 
 * 4) repeat 2) and 3) until all measurements are read
 * 
 * 5) call createStatistics with both commits
 * 
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

   public List<String> getKeys() {
      return data.keySet().stream().collect(Collectors.toList());
   }

   @JsonIgnore
   protected MeasurementConfig config;

   /*
    * Levelwise measurement maps the nodes ad-hoc for each node, and complete maps the nodes at the beginning; therefore, levelwise currently requires having the otherVersionNode
    * and the second tree structure available.
    * 
    * This should be refactored, so the trees are mapped in the beginning and afterwards only one tree structure is used.
    */
   private CallTreeNode otherCommitNode;

   /**
    * Creates a root node
    */
   public CallTreeNode(final String call, final String kiekerPattern, final String otherKiekerPattern, final MeasurementConfig config) {
      super(call, kiekerPattern, otherKiekerPattern);
      this.parent = null;
      this.config = config;
   }

   protected CallTreeNode(final String call, final String kiekerPattern, final String otherKiekerPattern, final CallTreeNode parent) {
      super(call, kiekerPattern, otherKiekerPattern);
      this.parent = parent;
      this.config = parent.config;
   }

   public void setConfig(final MeasurementConfig config) {
      this.config = config;
   }

   public MeasurementConfig getConfig() {
      return config;
   }

   @Override
   public List<CallTreeNode> getChildren() {
      return children;
   }

   public CallTreeNode appendChild(final String call, final String kiekerPattern, final String otherKiekerPattern) {
      final CallTreeNode added = new CallTreeNode(call, kiekerPattern, otherKiekerPattern, this);
      children.add(added);
      return added;
   }

   public CallTreeNode getParent() {
      return parent;
   }

   public void addMeasurement(final String commit, final Long duration) {
      checkDataAddPossible(commit);
      LOG.trace("Adding measurement: {} Call: {}", commit, call);
      CallTreeStatistics callTreeStatistics = data.get(commit);
      callTreeStatistics.addMeasurement(duration);
   }

   /**
    * Adds the measurement of *one full VM* to the measurements of the commit
    * 
    * @param commit
    * @param statistic
    */
   public void addAggregatedMeasurement(final String commit, final List<StatisticalSummary> statistic) {
      checkDataAddPossible(commit);
      StatisticUtil.removeWarmup(config.getNodeWarmup(), statistic);
      data.get(commit).addAggregatedMeasurement(statistic);
   }

   private void checkDataAddPossible(final String commit) {
      if (getOtherKiekerPattern() == null) {
         throw new RuntimeException("Other commit node needs to be defined before measurement! Node: " + call);
      }
      if (getOtherKiekerPattern().equals(CauseSearchData.ADDED) && commit.equals(config.getFixedCommitConfig().getCommit())) {
         LOG.error("Error occured in commit {}", commit);
         LOG.error("Node: {}", kiekerPattern);
         LOG.error("Other commit node: {}", getOtherKiekerPattern());
         throw new RuntimeException("Added methods may not contain data, trying to add data for " + commit);
      }
      if (call.equals(CauseSearchData.ADDED) && commit.equals(config.getFixedCommitConfig().getCommitOld())) {
         throw new RuntimeException("Added methods may not contain data, trying to add data for " + commit);
      }
   }

   public boolean hasMeasurement(final String commit) {
      return data.get(commit).getResults().size() > 0;
   }

   public List<OneVMResult> getResults(final String commit) {
      final CallTreeStatistics statistics = data.get(commit);
      return statistics != null ? statistics.getResults() : null;
   }

   public void initVMData(final String commit) {
      final CallTreeStatistics statistics = data.get(commit);
      LOG.debug("Adding VM: {} {} VMs: {}", call, commit, statistics.getResults().size());
      statistics.newResult();
   }

   private void newCommit(final String commit) {
      LOG.trace("Adding commit: {}", commit);
      CallTreeStatistics statistics = data.get(commit);
      if (statistics == null) {
         statistics = new CallTreeStatistics(config.getNodeWarmup());
         data.put(commit, statistics);
      }
   }

   public CompareData getComparableStatistics(final String commitOld, final String commit) {
      List<OneVMResult> dataOld = data.get(commitOld) != null ? data.get(commitOld).getResults() : null;
      List<OneVMResult> dataCurrent = data.get(commit) != null ? data.get(commit).getResults() : null;

      CompareData cd = CompareData.createCompareDataFromOneVMResults(dataOld, dataCurrent);
      return cd;
   }

   public SummaryStatistics getStatistics(final String commit) {
      LOG.trace("Getting data: {}", commit);
      final CallTreeStatistics statistics = data.get(commit);
      return statistics != null ? statistics.getStatistics() : null;
   }

   public void createStatistics(final String commit) {
      LOG.debug("Creating statistics: {} Call: {}", commit, call);
      final CallTreeStatistics callTreeStatistics = data.get(commit);
      callTreeStatistics.createStatistics(config.getStatisticsConfig());
      LOG.debug("Mean: " + callTreeStatistics.getStatistics().getMean() + " " + callTreeStatistics.getStatistics().getStandardDeviation());
   }

   @Override
   public String toString() {
      return kiekerPattern.toString();
   }

   private String extractMethodName(String call) {
      int lastParenthesisIndex = call.contains("(") ? call.lastIndexOf("(") : call.length() -1;
      String methodName = call.substring(0, lastParenthesisIndex);

      String[] parts = methodName.split(" ");
      return parts.length > 1 ? parts[parts.length - 1] : call;
   }

   public MethodCall toEntity() {
      if (call.equals(CauseSearchData.ADDED)) {
//         String otherKiekerPattern = getOtherKiekerPattern();
         String otherCall = getOtherCommitNode().getCall();
         return MethodCall.createMethodCallFromString(otherCall);
      } else {
         final int index = call.lastIndexOf(MethodCall.METHOD_SEPARATOR);
         String method = call.substring(index + 1);
         System.out.println(call + " " + method);
         final MethodCall entity;
         if (method.contains("(")) {
            entity = new MethodCall(call.substring(0, index), module, method.substring(0, method.indexOf('(')));
            entity.createParameters(method.substring(method.indexOf('(')));
         } else {
            entity = new MethodCall(call.substring(0, index), module, method);
         }
         entity.createParameters(getParameters());
         return entity;
      }
   }

   @JsonIgnore
   public TestcaseStatistic getTestcaseStatistic() {
      LOG.debug("Creating statistics for {} {} Keys: {}", config.getFixedCommitConfig().getCommit(), config.getFixedCommitConfig().getCommitOld(), data.keySet());
      final CallTreeStatistics currentStatistics = data.get(config.getFixedCommitConfig().getCommit());
      final SummaryStatistics current = currentStatistics.getStatistics();
      final CallTreeStatistics predecessorStatistics = data.get(config.getFixedCommitConfig().getCommitOld());
      final SummaryStatistics predecessor = predecessorStatistics.getStatistics();
      try {
         final TestcaseStatistic testcaseStatistic = new TestcaseStatistic(predecessor, current,
               predecessorStatistics.getCalls(), currentStatistics.getCalls());
         return testcaseStatistic;
      } catch (NumberIsTooSmallException t) {
         LOG.debug("Data: " + current.getN() + " " + predecessor.getN());
         final String otherCall = getOtherKiekerPattern() != null ? getOtherKiekerPattern() : "Not Existing";
         throw new RuntimeException("Could not read " + call + " Other Version: " + otherCall, t);
      } catch (TestcaseStatisticException t) {
         LOG.debug("Data: " + current.getN() + " " + predecessor.getN());
         final String otherCall = getOtherKiekerPattern() != null ? getOtherKiekerPattern() : "Not Existing";
         throw new RuntimeException("Could not read " + call + " Other Version: " + otherCall, t);
      }
   }

   @JsonIgnore
   public TestcaseStatistic getPartialTestcaseStatistic() {
      final CallTreeStatistics currentVersionStatistics = data.get(config.getFixedCommitConfig().getCommit());
      final SummaryStatistics current = currentVersionStatistics.getStatistics();
      final CallTreeStatistics previousVersionStatistics = data.get(config.getFixedCommitConfig().getCommitOld());
      final SummaryStatistics previous = previousVersionStatistics.getStatistics();

      if (firstHasValues(current, previous)) {
         final TestcaseStatistic testcaseStatistic = new TestcaseStatistic(previous, current, 0, currentVersionStatistics.getCalls());
         testcaseStatistic.setChange(true);
         return testcaseStatistic;
      } else if (firstHasValues(previous, current)) {
         final TestcaseStatistic testcaseStatistic = new TestcaseStatistic(previous, current, previousVersionStatistics.getCalls(), 0);
         testcaseStatistic.setChange(true);
         return testcaseStatistic;
      } else if ((current == null || current.getN() == 0) && (previous == null || previous.getN() == 0)) {
         LOG.error("Could not measure {}", this);
         final TestcaseStatistic testcaseStatistic = new TestcaseStatistic(previous, current, 0, 0);
         testcaseStatistic.setChange(true);
         return testcaseStatistic;
      } else {
         throw new RuntimeException("Partial statistics should exactly be created if one node is unmeasurable");
      }
   }

   private boolean firstHasValues(final SummaryStatistics first, final SummaryStatistics second) {
      return (second == null || second.getN() == 0) && (first != null && first.getN() > 0);
   }

   public void initCommitData() {
      FixedCommitConfig fixedCommitConfig = config.getFixedCommitConfig();
      LOG.debug("Init commit data: {}", fixedCommitConfig.getCommit(), fixedCommitConfig.getCommitOld());
      resetStatistics();
      newCommit(fixedCommitConfig.getCommitOld());
      newCommit(fixedCommitConfig.getCommit());
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
   public CallTreeNode getOtherCommitNode() {
      return otherCommitNode;
   }

   public void setOtherCommitNode(final CallTreeNode otherVersionNode) {
      this.otherCommitNode = otherVersionNode;
   }

   @JsonIgnore
   public String getMethod() {
      final String method = call.contains("#") ? call.substring(call.lastIndexOf('#')) : call;
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
   public int getEoi(String commit) {
      if (isAdded(commit)) {
         return -1;
      }
      int eoi;
      if (parent != null) {
         List<CallTreeNode> notAddedSiblingList = parent.getChildren()
               .stream()
               .filter(predecessor -> !predecessor.isAdded(commit))
               .collect(Collectors.toList());
         int predecessorIndex = notAddedSiblingList
               .indexOf(this) - 1;
         if (predecessorIndex >= 0) {
            CallTreeNode predecessor = notAddedSiblingList.get(predecessorIndex);
            int predecessorEOI = predecessor.getEoi(commit);
            eoi = predecessorEOI + predecessor.getAllChildCount(commit) + 1;
         } else {
            eoi = parent.getEoi(commit) + 1;
         }
      } else {
         eoi = 0;
      }
      return eoi;
   }

   private boolean isAdded(String commit) {
      FixedCommitConfig executionConfig = config.getFixedCommitConfig();
      if (executionConfig.getCommitOld().equals(commit) && call.equals(CauseSearchData.ADDED)) {
         return true;
      }
      String otherKiekerPattern = getOtherKiekerPattern();
      if (executionConfig.getCommit().equals(commit) && CauseSearchData.ADDED.equals(otherKiekerPattern)) {
         return true;
      }
      return false;
   }

   private int getAllChildCount(String commit) {
      int childs = 0;
      for (CallTreeNode child : children) {
         boolean currentVersionValidNode = !child.isAdded(commit);
         if (currentVersionValidNode) {
            int usableChildCount = child.getAllChildCount(commit);
            childs += usableChildCount + 1;
         }
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

   public long getCallCount(final String commit) {
      return data.get(commit).getResults().stream().mapToLong(result -> result.getCalls()).sum();
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

   @JsonIgnore
   public CallTreeNode getChildByKiekerPattern(String kiekerPattern) {
      for (CallTreeNode child : children) {
         if (child.getKiekerPattern().equals(kiekerPattern)) {
            return child;
         }
      }
      return null;

   }

}