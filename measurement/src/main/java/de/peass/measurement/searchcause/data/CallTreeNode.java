package de.peass.measurement.searchcause.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.measurement.analysis.statistics.DescribedChunk;
import de.peass.measurement.analysis.statistics.TestcaseStatistic;

public class CallTreeNode {

   private final String call;
   private final String kiekerPattern;

   private final CallTreeNode parent;
   private final List<CallTreeNode> children = new ArrayList<>();
   private Map<String, CallTreeStatistics> data = new HashMap<>();
   
   private String version, predecessor;

   public CallTreeNode(String call, String kiekerPattern, CallTreeNode parent) {
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

   public CallTreeNode append(String call, String kiekerPattern) {
      CallTreeNode added = new CallTreeNode(call, kiekerPattern, this);
      children.add(added);
      return added;
   }

   public CallTreeNode getParent() {
      return parent;
   }

   public String getKiekerPattern() {
      return kiekerPattern;
   }

   public void addMeasurement(String version, Long duration) {
      data.get(version).addMeasurement(duration);

   }

   public void newChunk(String version) {
      CallTreeStatistics statistics = data.get(version);
      if (statistics == null) {
         statistics = new CallTreeStatistics();
         data.put(version, statistics);
      }
      statistics.newChunk();
   }

   public DescriptiveStatistics getStatistics(String version) {
      return data.get(version).getStatistics();
   }

   public void createStatistics(String version, int warmup) {
      final CallTreeStatistics callTreeStatistics = data.get(version);
      callTreeStatistics.createStatistics(warmup);
   }

   @Override
   public String toString() {
      return kiekerPattern.toString();
   }

   public ChangedEntity toEntity() {
      int index = call.lastIndexOf(ChangedEntity.METHOD_SEPARATOR);
      ChangedEntity entity = new ChangedEntity(call.substring(0, index), "", call.substring(index + 1));
      return entity;
   }

   public TestcaseStatistic getTestcaseStatistic() {
      DescriptiveStatistics current = data.get(version).getStatistics();
      DescriptiveStatistics previous = data.get(predecessor).getStatistics();
      return new TestcaseStatistic(current, previous);
   }

   public void setVersions(String version, String predecessor) {
      this.version = version;
      this.predecessor = predecessor;
   }
}