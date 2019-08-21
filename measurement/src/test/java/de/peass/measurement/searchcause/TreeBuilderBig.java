package de.peass.measurement.searchcause;

import de.peass.measurement.searchcause.data.CallTreeNode;

class TreeBuilder {
   protected final CallTreeNode root = new CallTreeNode("Test#test", "public void Test.test", null);
   protected final CallTreeNode a = root.append("ClassA#methodA", "public void ClassA.methodA");
   protected final CallTreeNode b = a.append("ClassB#methodB", "public void ClassB.methodB");
   protected final CallTreeNode c = root.append("ClassC#methodC", "public void ClassC.methodC");

   public TreeBuilder() {

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
      buildChunks(root, "2", 95);
      buildChunks(a, "2", 95);
      buildChunks(c, "2", 100);
      buildChunks(b, "2", 95);

      buildChunks(root, "1", 105);
      buildChunks(a, "1", 105);
      buildChunks(c, "1", 100);
      buildChunks(b, "1", 105);
   }

   protected void buildStatistics(final CallTreeNode[] nodes) {
      for (final CallTreeNode node : nodes) {
         node.createStatistics("2");
         node.createStatistics("1");
      }
   }

   protected void initVersions(final CallTreeNode[] nodes) {
      for (final CallTreeNode node : nodes) {
         node.setVersions("2", "1");
      }
   }

   protected void buildChunks(final CallTreeNode node, final String version, final long average) {
      for (int i = 0; i < 3; i++) {
         node.newResult(version);
         node.addMeasurement(version, average + i % 2);
         node.addMeasurement(version, average + 5);
         node.addMeasurement(version, average - 5);
      }
   }
}

public class TreeBuilderBig extends TreeBuilder {

   final CallTreeNode b2 = c.append("ClassB#methodB", "public void ClassB.methodB");
   private final boolean secondBDiffering;

   public TreeBuilderBig(boolean secondBDiffering) {
      this.secondBDiffering = secondBDiffering;
   }

   @Override
   protected void buildMeasurements() {
      final CallTreeNode[] nodes = new CallTreeNode[] { root, a, b, c, b2 };
      initVersions(nodes);
      buildChunks(b2, "2", 95);
      buildBasicChunks();
      if (secondBDiffering) {
         buildChunks(b2, "1", 105);
      } else {
         buildChunks(b2, "1", 95);
      }

      buildStatistics(nodes);
   }
}

class TreeBuilderDifferent extends TreeBuilder {

   final CallTreeNode d = c.append("ClassD#methodD", "public void ClassD.methodD");
   final CallTreeNode e = c.append("ClassE#methodE", "public void ClassE.methodE");

   public TreeBuilderDifferent() {
   }

   @Override
   protected void buildMeasurements() {
      final CallTreeNode[] nodes = new CallTreeNode[] { root, a, b, c, d, e };
      initVersions(nodes);
      buildChunks(d, "2", 95);
      buildChunks(e, "2", 95);
      buildBasicChunks();
      buildChunks(d, "1", 105);
      buildChunks(e, "1", 105);

      buildStatistics(nodes);
   }
}
