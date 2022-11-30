package de.dagere.peass.visualization;

import java.util.HashSet;
import java.util.Set;

import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;

public class SingleNodePreparator implements INodePreparator {
   private CallTreeNode rootData;
   private final GraphNode rootGraph;

   public SingleNodePreparator(final CallTreeNode rootData) {
      this.rootData = rootData;

      rootGraph = new GraphNode(rootData.getCall(), rootData.getKiekerPattern(), CauseSearchData.ADDED);
      rootGraph.setModule(rootData.getModule());
   }

   public void prepare() {
      setGraphData(rootData, rootGraph);

      processNode(rootData, rootGraph);

      PrefixSetter.preparePrefix(rootGraph);
   }

   private void setGraphData(final CallTreeNode dataChild, final GraphNode newChild) {
      newChild.setName(dataChild.getCall());
   }

   private void processNode(final CallTreeNode measuredParent, final GraphNode graphParent) {
      // final EqualChildDeterminer determiner = new EqualChildDeterminer();
      final Set<String> addedPatterns = new HashSet<>();
      for (final CallTreeNode measuredChild : measuredParent.getChildren()) {
         final GraphNode newChild;
         newChild = new GraphNode(measuredChild.getCall(), measuredChild.getKiekerPattern(), CauseSearchData.ADDED);
         newChild.setModule(measuredChild.getModule());

         newChild.setParent(measuredParent.getCall());
         newChild.setEss(graphParent.getEss() + 1);
         setGraphData(measuredChild, newChild);

         graphParent.getChildren().add(newChild);
         processNode(measuredChild, newChild);
         addedPatterns.add(measuredChild.getKiekerPattern());

      }
   }

   @Override
   public GraphNode getGraphRoot() {
      return rootGraph;
   }
}
