package de.peass.measurement.searchcause.treeanalysis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import de.peass.measurement.searchcause.data.CallTreeNode;

public class TreeUtil {

   private static final Logger LOG = LogManager.getLogger(TreeUtil.class);

   public static boolean areTracesEqual(final CallTreeNode firstNode, final CallTreeNode secondNode) {
      if (!firstNode.getKiekerPattern().equals(secondNode.getKiekerPattern())) {
         return false;
      }
      if (firstNode.getChildren().size() != secondNode.getChildren().size()) {
         return false;
      }
      final Iterator<CallTreeNode> firstIt = firstNode.getChildren().iterator();
      final Iterator<CallTreeNode> secondIt = secondNode.getChildren().iterator();
      while (firstIt.hasNext() && secondIt.hasNext()) {
         final CallTreeNode firstChild = firstIt.next();
         final CallTreeNode secondChild = secondIt.next();
         if (!areTracesEqual(firstChild, secondChild)) {
            return false;
         }
      }

      return true;
   }

   public static int findMapping(final CallTreeNode firstNode, final CallTreeNode secondNode) {
      final Graph<CallTreeNode, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

      firstNode.getChildren().forEach(node -> graph.addVertex(node));
      secondNode.getChildren().forEach(node -> graph.addVertex(node));

      for (final CallTreeNode firstChild : firstNode.getChildren()) {
         for (final CallTreeNode secondChild : secondNode.getChildren()) {
            final DefaultWeightedEdge edge = graph.addEdge(firstChild, secondChild);
            int weight;
            if (firstChild.getKiekerPattern().equals(secondChild.getKiekerPattern())) {
               weight = 100;
            } else if (firstChild.getCall().equals(secondChild.getCall())) {
               weight = 10;
            } else {
               if (firstChild.getMethod().equals(secondChild.getMethod())) {
                  if (firstChild.getParameters().equals(secondChild.getParameters())) {
                     weight = 3;
                  } else {
                     weight = 2;
                  }
               } else {
                  weight = 1;
               }
            }
            System.out.println("Edge: " + firstChild + " " + secondChild + " - " + weight);
            graph.setEdgeWeight(edge, weight);
         }
      }

      final Set<CallTreeNode> partition1 = new HashSet<>(firstNode.getChildren());
      final Set<CallTreeNode> partition2 = new HashSet<>(secondNode.getChildren());

      final MaximumWeightBipartiteMatching<CallTreeNode, DefaultWeightedEdge> matching = new MaximumWeightBipartiteMatching<>(graph, partition1,
            partition2);
      for (final DefaultWeightedEdge edge : matching.getMatching()) {
         final CallTreeNode source = graph.getEdgeSource(edge);
         final CallTreeNode target = graph.getEdgeTarget(edge);
         source.setOtherVersionNode(target);
         target.setOtherVersionNode(source);
         LOG.info("Matched: {} - {}", source, target);
      }
      return matching.getMatching().getEdges().size();
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
}
