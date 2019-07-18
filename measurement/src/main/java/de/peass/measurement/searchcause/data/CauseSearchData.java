package de.peass.measurement.searchcause.data;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.peass.dependency.analysis.data.ChangedEntity;
import de.peass.dependency.analysis.data.TestCase;
import de.peass.measurement.MeasurementConfiguration;

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

   public void setTestcase(String testcase) {
      this.testcase = testcase;
   }

   public MeasurementConfiguration getConfig() {
      return config;
   }

   public void setConfig(MeasurementConfiguration config) {
      this.config = config;
   }

   public MeasuredNode getNodes() {
      return nodes;
   }

   public void setNodes(MeasuredNode nodes) {
      this.nodes = nodes;
   }

   public String getVersion() {
      return version;
   }

   public void setVersion(String version) {
      this.version = version;
   }

   public String getPredecessor() {
      return predecessor;
   }

   public void setPredecessor(String predecessor) {
      this.predecessor = predecessor;
   }

   @JsonIgnore
   public void addDiff(CallTreeNode rawDataNode) {
      MeasuredNode serializeNode = new MeasuredNode();
      serializeNode.setStatistic(rawDataNode.getTestcaseStatistic());
      serializeNode.setCall(rawDataNode.getCall());

      current.put(rawDataNode, serializeNode);

      if (rawDataNode.getParent() == null) {
         nodes = serializeNode;
      } else {
         MeasuredNode parent = current.get(rawDataNode.getParent());
         parent.getChilds().add(serializeNode);
      }
   }
}
