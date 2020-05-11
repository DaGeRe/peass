package de.peass.measurement.rca.helper;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import de.peass.dependency.execution.MeasurementConfiguration;
import de.peass.measurement.rca.data.CallTreeNode;

public class TreeBuilder {
   protected final CallTreeNode root = new CallTreeNode("Test#test", "public void Test.test");
   protected final CallTreeNode a = root.appendChild("ClassA#methodA", "public void ClassA.methodA");
   protected final CallTreeNode b = a.appendChild("ClassB#methodB", "public void ClassB.methodB");
   protected final CallTreeNode c = root.appendChild("ClassC#methodC", "public void ClassC.methodC");

   protected String version1 = "000001~1";
   protected String version2 = "000001";

   private boolean useFullLogAPI = true;

   MeasurementConfiguration config;

   public TreeBuilder(final MeasurementConfiguration config) {
      this(config, true);
   }

   public TreeBuilder(final MeasurementConfiguration config, final boolean useFullLogAPI) {
      this.config = config;
      this.useFullLogAPI = useFullLogAPI;
      
      root.setOtherVersionNode(new CallTreeNode("Test#test", "public void Test.test"));
      a.setOtherVersionNode(new CallTreeNode("ClassA#methodA", "public void ClassA.methodA"));
      b.setOtherVersionNode(new CallTreeNode("ClassA#methodB", "public void ClassA.methodB"));
      c.setOtherVersionNode(new CallTreeNode("ClassA#methodC", "public void ClassA.methodC"));
   }

   public TreeBuilder() {
      config = new MeasurementConfiguration(3);
      config.setIterations(3);
   }

   public CallTreeNode getRoot() {
      buildMeasurements();
      return root;
   }

   protected void buildMeasurements() {
      final CallTreeNode[] nodes = new CallTreeNode[] { root, a, b, c };
      initVersions(nodes);
      buildBasicChunks();
      buildStatistics(nodes);
   }

   protected void buildBasicChunks() {
      buildChunks(root, version2, 95);
      buildChunks(a, version2, 95);
      buildChunks(c, version2, 100);
      buildChunks(b, version2, 95);

      buildChunks(root, version1, 105);
      buildChunks(a, version1, 105);
      buildChunks(c, version1, 100);
      buildChunks(b, version1, 105);
   }

   protected void buildStatistics(final CallTreeNode[] nodes) {
      for (final CallTreeNode node : nodes) {
         node.createStatistics(version2);
         node.createStatistics(version1);
      }
   }

   protected void initVersions(final CallTreeNode[] nodes) {
      for (final CallTreeNode node : nodes) {
         node.setWarmup(config.getWarmup());
         node.setVersions(version2, version1);
      }
   }

   protected void buildChunks(final CallTreeNode node, final String version, final long average) {
      if (useFullLogAPI) {
         writeFullLogData(node, version, average);
      } else {
         writeAggregatedData(node, version, average);
      }

   }

   private void writeAggregatedData(final CallTreeNode node, final String version, final long average) {
      final List<StatisticalSummary> statistics = new LinkedList<>();
      for (int vm = 0; vm < config.getVms(); vm++) {
         final long deltaVM = (config.getVms() / 2) + vm * 2;
         for (int iteration = 0; iteration < config.getIterations(); iteration++) {
            final long deltaIteration = (config.getIterations() / 2) + iteration * 2;
            final long value = average - deltaIteration - deltaVM;
            final SummaryStatistics statistic = new SummaryStatistics();
            statistic.addValue(value);
            statistics.add(statistic);
         }
         node.setMeasurement(version, statistics);
      }
   }

   private void writeFullLogData(final CallTreeNode node, final String version, final long average) {
      for (int vm = 0; vm < config.getVms(); vm++) {
         node.newVM(version);
         final long deltaVM = (config.getVms() / 2) + vm * 2;
         for (int iteration = 0; iteration < config.getIterations(); iteration++) {
            final long deltaIteration = (config.getIterations() / 2) + iteration * 2;
            final long value = average - deltaIteration - deltaVM;
            node.addMeasurement(version, value);
         }
      }
   }

   public CallTreeNode getA() {
      return a;
   }
   
   public CallTreeNode getB() {
      return b;
   }
   
   public CallTreeNode getC() {
      return c;
   }
}