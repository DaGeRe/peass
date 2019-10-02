package de.peass.measurement.searchcause.treeanalysis;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.MatchingAlgorithm.Matching;
import org.jgrapht.alg.matching.MaximumWeightBipartiteMatching;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import de.peass.measurement.searchcause.data.CallTreeNode;
import de.peass.measurement.searchcause.serialization.MeasuredNode;

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
      final Graph<CallTreeNode, DefaultWeightedEdge> graph = buildGraph(firstNode, secondNode);

      buildEdges(firstNode, secondNode, graph);

      final Set<CallTreeNode> partition1 = new HashSet<>(firstNode.getChildren());
      final Set<CallTreeNode> partition2 = new HashSet<>(secondNode.getChildren());

      final MaximumWeightBipartiteMatching<CallTreeNode, DefaultWeightedEdge> matching = new MaximumWeightBipartiteMatching<>(graph, partition1,
            partition2);
      final Matching<CallTreeNode, DefaultWeightedEdge> resultMatching = matching.getMatching();
      for (final DefaultWeightedEdge edge : resultMatching) {
         final CallTreeNode source = graph.getEdgeSource(edge);
         final CallTreeNode target = graph.getEdgeTarget(edge);
         source.setOtherVersionNode(target);
         target.setOtherVersionNode(source);
         LOG.info("Matched: {} - {}", source, target);
      }
      return resultMatching.getEdges().size();
   }

   private static Graph<CallTreeNode, DefaultWeightedEdge> buildGraph(final CallTreeNode firstNode, final CallTreeNode secondNode) {
      final Graph<CallTreeNode, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

      firstNode.getChildren().forEach(node -> graph.addVertex(node));
      secondNode.getChildren().forEach(node -> graph.addVertex(node));
      return graph;
   }

   private static void buildEdges(final CallTreeNode firstNode, final CallTreeNode secondNode, final Graph<CallTreeNode, DefaultWeightedEdge> graph) {
      for (final CallTreeNode firstChild : firstNode.getChildren()) {
         for (final CallTreeNode secondChild : secondNode.getChildren()) {
            final DefaultWeightedEdge edge = graph.addEdge(firstChild, secondChild);
            double weight;
            if (firstChild.getKiekerPattern().equals(secondChild.getKiekerPattern())) {
               weight = 1000;
            } else if (firstChild.getCall().equals(secondChild.getCall())) {
               final int commonPrefixShare = getPrefixShare(firstChild, secondChild);
               weight = 300 + commonPrefixShare;
            } else {
               if (firstChild.getMethod().equals(secondChild.getMethod())) {
                  if (firstChild.getParameters().equals(secondChild.getParameters())) {
                     weight = 50;
                  } else {
                     final int commonPrefixShare = getPrefixShare(firstChild, secondChild);
                     weight = 3 + commonPrefixShare;
                  }
               } else {
                  weight = 1;
               }
            }
            final int index1 = firstChild.getPosition();
            final int index2 = secondChild.getPosition();
            if (index1 == index2) {
               weight += 0.1;
            }
            System.out.println("Edge: " + firstChild + " " + secondChild + " - " + weight);
            graph.setEdgeWeight(edge, weight);
         }
      }
   }

   private static int getPrefixShare(final CallTreeNode firstChild, final CallTreeNode secondChild) {
      final String commonPrefix = StringUtils.getCommonPrefix(firstChild.getParameters(), secondChild.getParameters());
      final int averageLength = (firstChild.getParameters().length() + secondChild.getParameters().length()) / 2;
      final int commonPrefixShare = (int) (10d * commonPrefix.length() / averageLength);
      return commonPrefixShare;
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
