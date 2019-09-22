package de.peass.measurement.searchcause.data;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.MeasurementConfiguration;
import de.peass.measurement.searchcause.serialization.MeasuredNode;

public class CauseSearchData {
   private String testcase;
   private String version, predecessor;
   private MeasurementConfiguration config;
   private MeasuredNode nodes;

   private Map<CallTreeNode, MeasuredNode> current = new HashMap<>();

   public CauseSearchData() {

   }

   public CauseSearchData(final TestCase test, final String version, final String predecessor, final MeasurementConfiguration config) {
      this.testcase = test.getClazz() + ChangedEntity.METHOD_SEPARATOR + test.getMethod();
      this.version = version;
      this.predecessor = predecessor;
      this.config = config;
   }

   public String getTestcase() {
      return testcase;
   }

   public void setTestcase(final String testcase) {
      this.testcase = testcase;
   }

   public MeasurementConfiguration getConfig() {
      return config;
   }

   public void setConfig(final MeasurementConfiguration config) {
      this.config = config;
   }

   public MeasuredNode getNodes() {
      return nodes;
   }

   public void setNodes(final MeasuredNode nodes) {
      this.nodes = nodes;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(final String version) {
      this.version = version;
   }

   public String getPredecessor() {
      return predecessor;
   }

   public void setPredecessor(final String predecessor) {
      this.predecessor = predecessor;
   }

   @JsonIgnore
   public MeasuredNode addDiff(final CallTreeNode rawDataNode) {
      final MeasuredNode serializeNode = new MeasuredNode();
      serializeNode.setStatistic(rawDataNode.getTestcaseStatistic());
      serializeNode.setCall(rawDataNode.getCall());
      serializeNode.setKiekerPattern(rawDataNode.getKiekerPattern());
      serializeNode.setOtherCall(rawDataNode.getOtherVersionNode() != null ? rawDataNode.getOtherVersionNode().getCall() : "UNKNOWN");

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
      serializeNode.setValues(rawDataNode, version, predecessor);
   }
}
