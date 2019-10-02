package de.peass.measurement.searchcause.helper;

import de.peass.measurement.searchcause.data.CallTreeNode;

public class TreeBuilder {
   protected final CallTreeNode root = new CallTreeNode("Test#test", "public void Test.test", null);
   protected final CallTreeNode a = root.appendChild("ClassA#methodA", "public void ClassA.methodA");
   protected final CallTreeNode b = a.appendChild("ClassB#methodB", "public void ClassB.methodB");
   protected final CallTreeNode c = root.appendChild("ClassC#methodC", "public void ClassC.methodC");

   protected String version1 = "000001~1";
   protected String version2 = "000001";
   
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
         node.setVersions(version2, version1);
      }
   }

   protected void buildChunks(final CallTreeNode node, final String version, final long average) {
      for (int i = 0; i < 3; i++) {
         node.newVM(version);
         node.addMeasurement(version, average + i % 2);
         node.addMeasurement(version, average + 5);
         node.addMeasurement(version, average - 5);
      }
   }
}