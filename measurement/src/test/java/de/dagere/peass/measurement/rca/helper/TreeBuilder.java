package de.dagere.peass.measurement.rca.helper;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import de.dagere.peass.config.MeasurementConfig;
import de.dagere.peass.measurement.rca.data.CallTreeNode;

public class TreeBuilder {
   
   public static final String VERSION_OLD = "000001~1";
   public static final String VERSION = "000001";
   
   protected final CallTreeNode root, a, b, c, constructor;

   private CallTreeNode d;
   private CallTreeNode e;

   protected String versionPredecessor = VERSION_OLD;
   protected String version = VERSION;

   private boolean useFullLogAPI = true;
   private boolean addOutlier = false;

   private final MeasurementConfig config;

   public TreeBuilder(final MeasurementConfig config) {
      this(config, true);
   }

   public TreeBuilder(final MeasurementConfig config, final boolean useFullLogAPI) {
      this.config = config;
      config.getExecutionConfig().setCommitOld(VERSION_OLD);
      config.getExecutionConfig().setCommit(VERSION);
      
      this.useFullLogAPI = useFullLogAPI;

      root = new CallTreeNode("Test#test", "public void Test.test()", "public void Test.test()", config);
      a = root.appendChild("ClassA#methodA", "public void ClassA.methodA()", "public void ClassA.methodA()");
      b = a.appendChild("ClassB#methodB", "public void ClassB.methodB()", "public void ClassB.methodB()");
      c = root.appendChild("ClassC#methodC", "public void ClassC.methodC()", "public void ClassC.methodC()");
      constructor = root.appendChild("ClassA#<init>", "new public void ClassA.<init>()", "new public void ClassA.<init>()");
   }

   public TreeBuilder() {
      this(new MeasurementConfig(3));
      config.setIterations(3);
   }

   public void setAddOutlier(final boolean addOutlier) {
      this.addOutlier = addOutlier;
   }

   public void addDE() {
      d = c.appendChild("ClassD#methodD", "public void ClassD.methodD()", "public void ClassD.methodD()");
      e = c.appendChild("ClassE#methodE", "public void ClassE.methodE()", "public void ClassE.methodE()");
   }

   public void buildMeasurements(final CallTreeNode... nodes) {
      initVersions(nodes);
      buildBasicChunks(nodes);
      buildStatistics(nodes);
   }

   protected void buildBasicChunks(final CallTreeNode[] nodes) {
      List<CallTreeNode> nodeList = Arrays.asList(nodes);
      if (nodeList.contains(root)) {
         buildChunks(root, version, 95);
         buildChunks(root, versionPredecessor, 105);
      }

      if (nodeList.contains(a)) {
         buildChunks(a, version, 95);
         buildChunks(a, versionPredecessor, 105);
      }

      if (nodeList.contains(b)) {
         buildChunks(b, version, 95);
         buildChunks(b, versionPredecessor, 105);
      }

      if (nodeList.contains(c)) {
         buildChunks(c, version, 100);
         buildChunks(c, versionPredecessor, 100);
      }

      if (nodeList.contains(constructor)) {
         buildChunks(constructor, version, 95);
         buildChunks(constructor, versionPredecessor, 95);
      }

      if (nodeList.contains(d) || (c.getChildren().size() > 0 && nodeList.contains(c.getChildren().get(0)))) {
         buildChunks(c.getChildren().get(0), version, 95);
      }

      if (nodeList.contains(e) || (c.getChildren().size() > 1 && nodeList.contains(c.getChildren().get(1)))) {
         buildChunks(c.getChildren().get(1), version, 95);
      }
   }

   protected void buildStatistics(final CallTreeNode[] nodes) {
      for (final CallTreeNode node : nodes) {
         node.createStatistics(version);
         node.createStatistics(versionPredecessor);
      }
   }

   protected void initVersions(final CallTreeNode[] nodes) {
      for (final CallTreeNode node : nodes) {
         node.initVersions();
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
         node.addAggregatedMeasurement(version, statistics);
      }
   }

   private void writeFullLogData(final CallTreeNode node, final String version, final long average) {
      for (int vm = 0; vm < config.getVms(); vm++) {
         node.newVM(version);
         for (int warmup = 0; warmup < config.getWarmup(); warmup++) {
            node.addMeasurement(version, average * 5);
         }
         for (int iteration = 0; iteration < config.getIterations(); iteration++) {
            final long value = getValue(average, vm, iteration);
            if (!addOutlier || vm != config.getVms() - 1) {
               node.addMeasurement(version, value);
            } else {
               node.addMeasurement(version, 100000L);
            }
         }
      }
   }

   private long getValue(final long average, final int vm, final int iteration) {
      final long deltaVM = (config.getVms() / 2) - vm;
      final long deltaIteration = (config.getIterations() / 2) - iteration;
      final long value = average - deltaIteration - deltaVM;
      return value;
   }

   public CallTreeNode getRoot() {
      return root;
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

   public CallTreeNode getD() {
      return d;
   }

   public CallTreeNode getE() {
      return e;
   }

   public CallTreeNode getConstructor() {
      return constructor;
   }
}