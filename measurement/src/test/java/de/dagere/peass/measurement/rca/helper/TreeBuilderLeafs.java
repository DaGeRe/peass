package de.dagere.peass.measurement.rca.helper;

import de.dagere.peass.measurement.rca.data.CallTreeNode;

public class TreeBuilderLeafs extends TreeBuilder {

   final CallTreeNode b2 = a.appendChild("ClassB#methodB", "public void ClassB.methodB", null);
   final CallTreeNode b3 = a.appendChild("ClassB#methodB", "public void ClassB.methodB", null);
   final CallTreeNode b4 = a.appendChild("ClassB#methodB", "public void ClassB.methodB", null);
   final CallTreeNode c2 = root.appendChild("ClassC#methodC", "public void ClassC.methodC", null);

   public TreeBuilderLeafs() {
   }

   @Override
   public void buildMeasurements(CallTreeNode... nodes) {
//      final CallTreeNode[] nodes = new CallTreeNode[] { root, a, b, c, b2, b3, b4, c2 };
      initVersions(nodes);
      buildChunks(b2, commit, 95);
      buildChunks(b3, commit, 95);
      buildChunks(b4, commit, 95);
      buildChunks(c2, commit, 95);
      buildBasicChunks(nodes);
      buildChunks(b2, commitPredecessor, 105);
      buildChunks(b3, commitPredecessor, 105);
      buildChunks(b4, commitPredecessor, 105);
      buildChunks(c2, commitPredecessor, 95);

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