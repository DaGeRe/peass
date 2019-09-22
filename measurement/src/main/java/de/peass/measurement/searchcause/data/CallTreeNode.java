package de.peass.measurement.searchcause.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.measurement.analysis.statistics.TestcaseStatistic;

class CallTreeNodeDeserializer extends JsonDeserializer<CallTreeNode> {

   @Override
   public CallTreeNode deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
      final JsonNode node = p.getCodec().readTree(p);
      final String call = node.get("call").asText();
      final String kiekerPattern = node.get("kiekerPattern").asText();
      final JsonNode children = node.get("children");
      final CallTreeNode root = new CallTreeNode(call, kiekerPattern, null);
      handleChild(children, root);
      
      return root;
   }

   private void handleChild(final JsonNode children, final CallTreeNode parent) {
      for (final JsonNode child : children) {
         final String call = child.get("call").asText();
         final String kiekerPattern = child.get("kiekerPattern").asText();
         final CallTreeNode created = parent.appendChild(call, kiekerPattern);
         handleChild(child.get("children"), created);
      }
   }
}

@JsonDeserialize(using = CallTreeNodeDeserializer.class)
public class CallTreeNode {

   private static final Logger LOG = LogManager.getLogger(CallTreeNode.class);

   private final String call;
   private final String kiekerPattern;

   @JsonIgnore
   private final CallTreeNode parent;
   protected final List<CallTreeNode> children = new ArrayList<>();
   protected final Map<String, CallTreeStatistics> data = new HashMap<>();

   protected String version, predecessor;

   private int warmup;

   private CallTreeNode otherVersionNode;

   public CallTreeNode(final String call, final String kiekerPattern, final CallTreeNode parent) {
      super();
      if (!kiekerPattern.contains(call.replace("#", "."))) {
         throw new RuntimeException("Pattern " + kiekerPattern + " must contain " + call);
      }
      if (kiekerPattern.contains("<init>") && !kiekerPattern.contains("new")) {
         throw new RuntimeException("Pattern " + kiekerPattern + " not legal - Constructor must contain new as return type!");
      }
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

   public CallTreeNode appendChild(final String call, final String kiekerPattern) {
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

   public void setMeasurement(final String version, final StatisticalSummary statistic) {
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
      LOG.debug("Adding version: {}", version);
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

   @JsonIgnore
   public TestcaseStatistic getTestcaseStatistic() {
      final SummaryStatistics current = data.get(version).getStatistics();
      final SummaryStatistics previous = data.get(predecessor).getStatistics();
      return new TestcaseStatistic(current, previous);
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
}