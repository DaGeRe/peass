package de.peass.measurement.searchcause.helper;

import de.peass.measurement.searchcause.data.CallTreeNode;

public class TreeBuilderBig extends TreeBuilder {

   final CallTreeNode b2 = c.appendChild("ClassB#methodB", "public void ClassB.methodB");
   private final boolean secondBDiffering;

   public TreeBuilderBig(final boolean secondBDiffering) {
      this.secondBDiffering = secondBDiffering;
   }

   @Override
   protected void buildMeasurements() {
      final CallTreeNode[] nodes = new CallTreeNode[] { root, a, b, c, b2 };
      initVersions(nodes);
      buildChunks(b2, version2, 95);
      buildBasicChunks();
      if (secondBDiffering) {
         buildChunks(b2, version1, 105);
      } else {
         buildChunks(b2, version1, 95);
      }

      buildStatistics(nodes);
   }
}
