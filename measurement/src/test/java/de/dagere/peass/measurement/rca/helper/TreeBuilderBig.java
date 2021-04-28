package de.dagere.peass.measurement.rca.helper;

import de.dagere.peass.measurement.rca.data.CallTreeNode;

public class TreeBuilderBig extends TreeBuilder {

   final CallTreeNode b2 = c.appendChild("ClassB#methodB", "public void ClassB.methodB()", null);
   private final boolean secondBDiffering;

   public TreeBuilderBig(final boolean secondBDiffering) {
      this.secondBDiffering = secondBDiffering;
   }

   @Override
   public void buildMeasurements(final CallTreeNode... nodes) {
//      final CallTreeNode[] nodes = new CallTreeNode[] { root, a, b, c, constructor, b2 };
      initVersions(nodes);
      buildChunks(b2, version, 95);
      buildBasicChunks(nodes);
      if (secondBDiffering) {
         buildChunks(b2, versionPredecessor, 105);
      } else {
         buildChunks(b2, versionPredecessor, 95);
      }

      buildStatistics(nodes);
   }
   
   public CallTreeNode getB2() {
      return b2;
   }
}
