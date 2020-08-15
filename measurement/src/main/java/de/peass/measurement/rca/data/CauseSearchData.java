package de.peass.measurement.rca.data;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.measurement.rca.CauseSearcherConfig;
import de.peass.measurement.rca.serialization.MeasuredNode;

public class CauseSearchData {

   private static final Logger LOG = LogManager.getLogger(CauseSearchData.class);

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
      buildCurrentMap(nodes, null);
   }

   private void buildCurrentMap(final MeasuredNode node, CallTreeNode parentStructure) {
      final CallTreeNode nodeStructure = parentStructure != null ? parentStructure.appendChild(node.getCall(), node.getKiekerPattern())
            : new CallTreeNode(node.getCall(), node.getKiekerPattern());
      current.put(nodeStructure, node);
      for (final MeasuredNode child : node.getChilds()) {
         buildCurrentMap(child, nodeStructure);
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
      try {
         final MeasuredNode serializeNode = buildSerializedNode(rawDataNode);
         if (!rawDataNode.getCall().equals("ADDED") &&
               !rawDataNode.getCall().equals("REMOVED") &&
               !rawDataNode.getOtherVersionNode().getCall().equals("ADDED") &&
               !rawDataNode.getOtherVersionNode().getCall().equals("REMOVED")) {
            System.out.println(rawDataNode.getCall() + " " + rawDataNode.getOtherVersionNode().getCall());
            serializeNode.setStatistic(rawDataNode.getTestcaseStatistic());
         } else {
            serializeNode.setStatistic(rawDataNode.getPartialTestcaseStatistic());
         }
         current.put(rawDataNode, serializeNode);

         if (rawDataNode.getParent() == null) {
            nodes = serializeNode;
         } else {
            final MeasuredNode parent = current.get(rawDataNode.getParent());
            parent.getChilds().add(serializeNode);
         }
         return serializeNode;
      } catch (Exception e) {
         throw e;
      }
   }

   private MeasuredNode buildSerializedNode(final CallTreeNode rawDataNode) {
      final MeasuredNode serializeNode = new MeasuredNode();
      serializeNode.setCall(rawDataNode.getCall());
      serializeNode.setKiekerPattern(rawDataNode.getKiekerPattern());
      serializeNode.setOtherKiekerPattern(rawDataNode.getOtherVersionNode() != null ? rawDataNode.getOtherVersionNode().getKiekerPattern() : "UNKNOWN");
      return serializeNode;
   }

   @JsonIgnore
   public void addDetailDiff(final CallTreeNode rawDataNode) {
      final MeasuredNode serializeNode = addDiff(rawDataNode);
      serializeNode.setValues(rawDataNode, measurementConfig.getVersion(), measurementConfig.getVersionOld());
   }
}
