package de.dagere.peass.measurement.rca.treeanalysis;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.MatchingAlgorithm.Matching;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultWeightedEdge;

import de.dagere.peass.measurement.rca.data.BasicNode;
import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.data.CauseSearchData;
import de.dagere.peass.measurement.rca.serialization.MeasuredNode;

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

   public static BidiMap<CallTreeNode, CallTreeNode> findChildMapping(final CallTreeNode currentNode, final CallTreeNode predecessorNode) {
      final MatchingTreeBuilder builder = new MatchingTreeBuilder(currentNode, predecessorNode);
      final Graph<CallTreeNodeVertex, DefaultWeightedEdge> graph = builder.getGraph();

      builder.buildEdges(currentNode, predecessorNode, graph);

      final Set<CallTreeNodeVertex> partitionCurrent = builder.getPartitionCurrent();
      final Set<CallTreeNodeVertex> partitionPredecessor = builder.getPartitionPrecedessor();

      final BidiMap<CallTreeNode, CallTreeNode> resultMatching = setOtherVersionNodes(graph, partitionCurrent, partitionPredecessor);
      if (partitionCurrent.size() > partitionPredecessor.size()) {
         addAdded(predecessorNode, currentNode.getChildren(), resultMatching);
      }
      if (partitionPredecessor.size() > partitionCurrent.size()) {
         addRemoved(currentNode, predecessorNode.getChildren(), resultMatching);
      }

      return resultMatching;
   }

   private static BidiMap<CallTreeNode, CallTreeNode> setOtherVersionNodes(final Graph<CallTreeNodeVertex, DefaultWeightedEdge> graph,
         final Set<CallTreeNodeVertex> partitionCurrent, final Set<CallTreeNodeVertex> partitionPredecessor) {
      final MaximumWeightBipartiteMatching<CallTreeNodeVertex, DefaultWeightedEdge> matching = new MaximumWeightBipartiteMatching<>(graph, partitionCurrent,
            partitionPredecessor);
      final Matching<CallTreeNodeVertex, DefaultWeightedEdge> resultMatching = matching.getMatching();
      BidiMap<CallTreeNode, CallTreeNode> mapping = new DualHashBidiMap<>();
      for (final DefaultWeightedEdge edge : resultMatching) {
         final CallTreeNode source = graph.getEdgeSource(edge).getNode();
         final CallTreeNode target = graph.getEdgeTarget(edge).getNode();
         mapping.put(source, target);
         source.setOtherKiekerPattern(target.getKiekerPattern());
         target.setOtherKiekerPattern(source.getKiekerPattern());
         
//         LOG.info("Matched: {} - {}", source, target);
      }
      return mapping;
   }

   private static void addAdded(final CallTreeNode otherParent, final List<CallTreeNode> partition, Map<CallTreeNode, CallTreeNode> mapping) {
      for (final CallTreeNode unmatched : partition) {
         if (unmatched.getOtherKiekerPattern() == null) {
            final CallTreeNode virtual_node = otherParent.appendChild(CauseSearchData.ADDED, CauseSearchData.ADDED, unmatched.getKiekerPattern());
            mapping.put(virtual_node, unmatched);
            unmatched.setOtherKiekerPattern(CauseSearchData.ADDED);
         }
      }
   }
   
   private static void addRemoved(final CallTreeNode otherParent, final List<CallTreeNode> partition, Map<CallTreeNode, CallTreeNode> mapping) {
      for (final CallTreeNode unmatched : partition) {
         if (unmatched.getOtherKiekerPattern() == null) {
            final CallTreeNode virtual_node = otherParent.appendChild(CauseSearchData.ADDED, CauseSearchData.ADDED, unmatched.getKiekerPattern());
            mapping.put(unmatched, virtual_node);
            unmatched.setOtherKiekerPattern(CauseSearchData.ADDED);
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
            firstChild.setOtherKiekerPattern(secondChild.getKiekerPattern());
            secondChild.setOtherKiekerPattern(firstChild.getOtherKiekerPattern());
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
            firstChild.setOtherKiekerPattern(secondChild.getKiekerPattern());
            secondChild.setOtherKiekerPattern(firstChild.getOtherKiekerPattern());
         }
      }
      return true;
   }
}
