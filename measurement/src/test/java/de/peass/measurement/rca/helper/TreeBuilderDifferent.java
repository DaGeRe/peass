package de.peass.measurement.rca.helper;

import de.peass.measurement.rca.data.CallTreeNode;

public class TreeBuilderDifferent extends TreeBuilder {

   final CallTreeNode d = c.appendChild("ClassD#methodD", "public void ClassD.methodD");
   final CallTreeNode e = c.appendChild("ClassE#methodE", "public void ClassE.methodE");

   public TreeBuilderDifferent() {
   }

   @Override
   protected void buildMeasurements() {
      final CallTreeNode[] nodes = new CallTreeNode[] { root, a, b, c, d, e };
      initVersions(nodes);
      buildChunks(d, version2, 95);
      buildChunks(e, version2, 95);
      buildBasicChunks();
      buildChunks(d, version1, 105);
      buildChunks(e, version1, 105);

      buildStatistics(nodes);
   }

   public CallTreeNode getD() {
      return d;
   }
   
   public CallTreeNode getE() {
      return e;
   }
}