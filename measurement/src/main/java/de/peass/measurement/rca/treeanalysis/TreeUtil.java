package de.peass.measurement.rca.treeanalysis;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.MatchingAlgorithm.Matching;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultWeightedEdge;

import de.peass.measurement.rca.data.BasicNode;
import de.peass.measurement.rca.data.CallTreeNode;
import de.peass.measurement.rca.data.CauseSearchData;
import de.peass.measurement.rca.serialization.MeasuredNode;

public class TreeUtil {

   private static final Logger LOG = LogManager.getLogger(TreeUtil.class);
   
   public static boolean areTracesEqual(final BasicNode firstNode, final BasicNode secondNode) {
      if (!firstNode.getKiekerPattern().equals(secondNode.getKiekerPattern())) {
         return false;
      }
      if (firstNode.getChildren().size() != secondNode.getChildren().size()) {
         return false;
      }
      final Iterator<? extends BasicNode> firstIt = firstNode.getChildren().iterator();
      final Iterator<? extends BasicNode> secondIt = secondNode.getChildren().iterator();
      while (firstIt.hasNext() && secondIt.hasNext()) {
         final BasicNode firstChild = firstIt.next();
         final BasicNode secondChild = secondIt.next();
         if (!areTracesEqual(firstChild, secondChild)) {
            return false;
         }
      }

      return true;
   }

   // Prevents hashCode and equals from CallTreeNode to be used
   public static class CallTreeNodeVertex {
      final CallTreeNode node;
      
      public CallTreeNodeVertex(final CallTreeNode node) {
         this.node = node;
      }
      
      public CallTreeNode getNode() {
         return node;
      }
   }

   public static int findChildMapping(final CallTreeNode firstNode, final CallTreeNode secondNode) {
      final MatchingTreeBuilder builder = new MatchingTreeBuilder(firstNode, secondNode);
      final Graph<CallTreeNodeVertex, DefaultWeightedEdge> graph = builder.getGraph();

      builder.buildEdges(firstNode, secondNode, graph);

      final Set<CallTreeNodeVertex> partition1 = builder.getPartition1();
      final Set<CallTreeNodeVertex> partition2 = builder.getPartition2();

      final Matching<CallTreeNodeVertex, DefaultWeightedEdge> resultMatching = setOtherVersionNodes(graph, partition1, partition2);
      if (partition1.size() > partition2.size()) {
         addSurplus(secondNode, firstNode.getChildren());
      }
      if (partition2.size() > partition1.size()) {
         addSurplus(firstNode, secondNode.getChildren());
      }

      return resultMatching.getEdges().size();
   }

   private static Matching<CallTreeNodeVertex, DefaultWeightedEdge> setOtherVersionNodes(final Graph<CallTreeNodeVertex, DefaultWeightedEdge> graph,
         final Set<CallTreeNodeVertex> partition1, final Set<CallTreeNodeVertex> partition2) {
      final MaximumWeightBipartiteMatching<CallTreeNodeVertex, DefaultWeightedEdge> matching = new MaximumWeightBipartiteMatching<>(graph, partition1,
            partition2);
      final Matching<CallTreeNodeVertex, DefaultWeightedEdge> resultMatching = matching.getMatching();
      for (final DefaultWeightedEdge edge : resultMatching) {
         final CallTreeNode source = graph.getEdgeSource(edge).getNode();
         final CallTreeNode target = graph.getEdgeTarget(edge).getNode();
         source.setOtherVersionNode(target);
         target.setOtherVersionNode(source);
         LOG.info("Matched: {} - {}", source, target);
      }
      return resultMatching;
   }

   private static void addSurplus(final CallTreeNode otherParent, final List<CallTreeNode> partition) {
      for (final CallTreeNode unmatched : partition) {
         if (unmatched.getOtherVersionNode() == null) {
            final CallTreeNode virtual_node = otherParent.appendChild(CauseSearchData.ADDED, CauseSearchData.ADDED);
            unmatched.setOtherVersionNode(virtual_node);
            virtual_node.setOtherVersionNode(unmatched);
         }
      }
   }

   public static boolean childrenEqual(final CallTreeNode firstNode, final CallTreeNode secondNode) {
      final Iterator<CallTreeNode> firstIt = firstNode.getChildren().iterator();
      final Iterator<CallTreeNode> secondIt = secondNode.getChildren().iterator();
      while (firstIt.hasNext() && secondIt.hasNext()) {
         final CallTreeNode firstChild = firstIt.next();
         final CallTreeNode secondChild = secondIt.next();
         if (!firstChild.getKiekerPattern().equals(secondChild.getKiekerPattern())) {
            return false;
         } else {
            firstChild.setOtherVersionNode(secondChild);
            secondChild.setOtherVersionNode(firstChild);
         }
      }
      return true;
   }

   public static boolean childrenEqual(final MeasuredNode firstNode, final MeasuredNode secondNode) {
      final Iterator<MeasuredNode> firstIt = firstNode.getChilds().iterator();
      final Iterator<MeasuredNode> secondIt = secondNode.getChilds().iterator();
      while (firstIt.hasNext() && secondIt.hasNext()) {
         final MeasuredNode firstChild = firstIt.next();
         final MeasuredNode secondChild = secondIt.next();
         if (!firstChild.getKiekerPattern().equals(secondChild.getKiekerPattern())) {
            return false;
         } else {
            // firstChild.setOtherVersionNode(secondChild);
            // secondChild.setOtherVersionNode(firstChild);
         }
      }
      return true;
   }
}
