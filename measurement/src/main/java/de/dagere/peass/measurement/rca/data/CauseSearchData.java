package de.dagere.peass.measurement.rca.data;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.dagere.peass.config.MeasurementConfiguration;
import de.dagere.peass.dependency.analysis.data.ChangedEntity;
import de.dagere.peass.measurement.rca.CauseSearcherConfig;
import de.dagere.peass.measurement.rca.serialization.MeasuredNode;

public class CauseSearchData {

   public static final String ADDED = "ADDED";

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

   private void buildCurrentMap(final MeasuredNode node, final CallTreeNode parentStructure) {
      final CallTreeNode nodeStructure = parentStructure != null ? 
            parentStructure.appendChild(node.getCall(), node.getKiekerPattern(), node.getOtherKiekerPattern())
            : new CallTreeNode(node.getCall(), node.getKiekerPattern(), node.getOtherKiekerPattern(), (MeasurementConfiguration) null);
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
      return causeConfig.getTestCase().getClazz() + ChangedEntity.METHOD_SEPARATOR + causeConfig.getTestCase().getMethod();
   }

   @JsonIgnore
   public MeasuredNode addDiff(final CallTreeNode rawDataNode) {
      final MeasuredNode serializeNode = buildSerializedNode(rawDataNode);
      setStatistic(rawDataNode, serializeNode);
      current.put(rawDataNode, serializeNode);

      if (rawDataNode.getParent() == null) {
         nodes = serializeNode;
      } else {
         final MeasuredNode parent = current.get(rawDataNode.getParent());
         if (parent != null) {
            parent.getChilds().add(serializeNode);
         }
      }
      return serializeNode;
   }

   private void setStatistic(final CallTreeNode rawDataNode, final MeasuredNode serializeNode) {
      if (!rawDataNode.getCall().equals(ADDED) &&
            !rawDataNode.getOtherVersionNode().getCall().equals(ADDED)) {
         LOG.debug("Setting statistic: " + rawDataNode.getCall() + " " + rawDataNode.getOtherVersionNode().getCall());
         serializeNode.setStatistic(rawDataNode.getTestcaseStatistic());
      } else {
         LOG.debug(rawDataNode.getCall() + " " + rawDataNode.getOtherVersionNode().getCall());
         serializeNode.setStatistic(rawDataNode.getPartialTestcaseStatistic());
      }
   }

   private MeasuredNode buildSerializedNode(final CallTreeNode rawDataNode) {
      final MeasuredNode serializeNode = new MeasuredNode(rawDataNode.getCall(), rawDataNode.getKiekerPattern(), null);
      serializeNode.setModule(rawDataNode.getModule());
      serializeNode.setOtherKiekerPattern(rawDataNode.getOtherVersionNode() != null ? rawDataNode.getOtherVersionNode().getKiekerPattern() : "UNKNOWN");
      return serializeNode;
   }

   @JsonIgnore
   public void addDetailDiff(final CallTreeNode rawDataNode) {
      final MeasuredNode serializeNode = addDiff(rawDataNode);
      serializeNode.setValues(rawDataNode, measurementConfig.getVersion(), measurementConfig.getVersionOld());
   }
}
