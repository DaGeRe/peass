package de.peass.measurement.rca.data;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.serialization.MeasuredNode;

public class CauseSearchData {
   private MeasurementConfiguration measurementConfig;
   private CauseSearcherConfig causeConfig;
   private MeasuredNode nodes;

   private Map<CallTreeNode, MeasuredNode> current = new HashMap<>();

   public CauseSearchData() {

   }

   public CauseSearchData(final MeasurementConfiguration config, final CauseSearcherConfig causeConfig) {
      this.measurementConfig = config;
      this.causeConfig = causeConfig;
   }

   public MeasurementConfiguration getMeasurementConfig() {
      return measurementConfig;
   }

   public void setConfig(final MeasurementConfiguration config) {
      this.measurementConfig = config;
   }

   public MeasuredNode getNodes() {
      return nodes;
   }

   public void setNodes(final MeasuredNode nodes) {
      this.nodes = nodes;
      buildCurrentMap(nodes);
   }

   private void buildCurrentMap(final MeasuredNode parent) {
      current.put(new CallTreeNode(parent.getCall(), parent.getKiekerPattern()), parent);
      for (final MeasuredNode node : parent.getChilds()) {
         buildCurrentMap(node);
      }
   }

   public CauseSearcherConfig getCauseConfig() {
      return causeConfig;
   }

   public void setCauseConfig(final CauseSearcherConfig causeConfig) {
      this.causeConfig = causeConfig;
   }

   @JsonIgnore
   public String getTestcase() {
      return causeConfig.getTestCase().getClazz() + "#" + causeConfig.getTestCase().getMethod();
   }

   @JsonIgnore
   public MeasuredNode addDiff(final CallTreeNode rawDataNode) {
      final MeasuredNode serializeNode = new MeasuredNode();
      serializeNode.setStatistic(rawDataNode.getTestcaseStatistic());
      serializeNode.setCall(rawDataNode.getCall());
      serializeNode.setKiekerPattern(rawDataNode.getKiekerPattern());
      serializeNode.setOtherKiekerPattern(rawDataNode.getOtherVersionNode() != null ? rawDataNode.getOtherVersionNode().getKiekerPattern() : "UNKNOWN");

      current.put(rawDataNode, serializeNode);

      if (rawDataNode.getParent() == null) {
         nodes = serializeNode;
      } else {
         final MeasuredNode parent = current.get(rawDataNode.getParent());
         parent.getChilds().add(serializeNode);
      }
      return serializeNode;
   }

   @JsonIgnore
   public void addDetailDiff(final CallTreeNode rawDataNode) {
      final MeasuredNode serializeNode = addDiff(rawDataNode);
      serializeNode.setValues(rawDataNode, measurementConfig.getVersion(), measurementConfig.getVersionOld());
   }
}
