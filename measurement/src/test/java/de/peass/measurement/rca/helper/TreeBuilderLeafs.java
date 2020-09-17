package de.peass.measurement.rca.helper;

import de.peass.measurement.rca.data.CallTreeNode;

public class TreeBuilderLeafs extends TreeBuilder {

   final CallTreeNode b2 = a.appendChild("ClassB#methodB", "public void ClassB.methodB");
   final CallTreeNode b3 = a.appendChild("ClassB#methodB", "public void ClassB.methodB");
   final CallTreeNode b4 = a.appendChild("ClassB#methodB", "public void ClassB.methodB");
   final CallTreeNode c2 = root.appendChild("ClassC#methodC", "public void ClassC.methodC");

   public TreeBuilderLeafs() {
   }

   @Override
   protected void buildMeasurements() {
      final CallTreeNode[] nodes = new CallTreeNode[] { root, a, b, c, b2, b3, b4, c2 };
      initVersions(nodes);
      buildChunks(b2, version, 95);
      buildChunks(b3, version, 95);
      buildChunks(b4, version, 95);
      buildChunks(c2, version, 95);
      buildBasicChunks();
      buildChunks(b2, versionPredecessor, 105);
      buildChunks(b3, versionPredecessor, 105);
      buildChunks(b4, versionPredecessor, 105);
      buildChunks(c2, versionPredecessor, 95);

      buildStatistics(nodes);
   }

   public CallTreeNode getB2() {
      return b2;
   }

   public CallTreeNode getB3() {
      return b3;
   }

   public CallTreeNode getB4() {
      return b4;
   }

   public CallTreeNode getC2() {
      return c2;
   }

}