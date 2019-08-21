package de.peass.measurement.searchcause.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.measurement.analysis.statistics.TestcaseStatistic;

public class CallTreeNode {

   private static final Logger LOG = LogManager.getLogger(CallTreeNode.class);
   
   private final String call;
   private final String kiekerPattern;

   private final CallTreeNode parent;
   private final List<CallTreeNode> children = new ArrayList<>();
   private final Map<String, CallTreeStatistics> data = new HashMap<>();
   
   private String version, predecessor;
   
   private int warmup;

   public CallTreeNode(final String call, final String kiekerPattern, final CallTreeNode parent) {
      super();
      this.parent = parent;
      this.kiekerPattern = kiekerPattern;
      this.call = call;
   }

   public String getCall() {
      return call;
   }

   public List<CallTreeNode> getChildren() {
      return children;
   }

   public CallTreeNode append(final String call, final String kiekerPattern) {
      final CallTreeNode added = new CallTreeNode(call, kiekerPattern, this);
      children.add(added);
      return added;
   }

   public CallTreeNode getParent() {
      return parent;
   }

   public String getKiekerPattern() {
      return kiekerPattern;
   }

   public void addMeasurement(final String version, final Long duration) {
      data.get(version).addMeasurement(duration);

   }

   public void newResult(final String version) {
      LOG.debug("Adding result: {}", version);
      CallTreeStatistics statistics = data.get(version);
      if (statistics == null) {
         statistics = new CallTreeStatistics(warmup);
         data.put(version, statistics);
      }
      statistics.newResult();
   }
   
   public void setWarmup(final int warmup){
      this.warmup = warmup;
   }

   public SummaryStatistics getStatistics(final String version) {
      LOG.debug("Getting data: {}", version);
      return data.get(version).getStatistics();
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

   public TestcaseStatistic getTestcaseStatistic() {
      final SummaryStatistics current = data.get(version).getStatistics();
      final SummaryStatistics previous = data.get(predecessor).getStatistics();
      return new TestcaseStatistic(current, previous);
   }

   public void setVersions(final String version, final String predecessor) {
      this.version = version;
      this.predecessor = predecessor;
   }

   public int getTreeSize() {
      int size = 1;
      for (final CallTreeNode child : children) {
         size+=child.getTreeSize();
      }
      return size;
   }

   public void resetStatistics() {
      data.values().forEach(statistics -> statistics.resetResults());
   }
}