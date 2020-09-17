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
      
      buildBasicChunks();
      buildChunks(d, version, 95);
      buildChunks(e, version, 95);
//      
//      buildChunks(d, versionPredecessor, 105);
//      buildChunks(e, versionPredecessor, 105);

      buildStatistics(nodes);
   }

   public CallTreeNode getD() {
      return d;
   }
   
   public CallTreeNode getE() {
      return e;
   }
}