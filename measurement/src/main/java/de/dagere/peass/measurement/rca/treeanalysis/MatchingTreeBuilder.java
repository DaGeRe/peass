package de.dagere.peass.measurement.rca.treeanalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import de.dagere.peass.measurement.rca.data.CallTreeNode;
import de.dagere.peass.measurement.rca.treeanalysis.TreeUtil.CallTreeNodeVertex;

/**
 * CallTreeNodes need to be equal if their KiekerPatterns are equal, except for the matching - here, the CallTreeNodes of each partition (version) need to be considered different.
 * Therefore they are identified by their index in the MatchingTreeBuilder.
 * 
 * @author reichelt
 *
 */
public class MatchingTreeBuilder {

   private static final Logger LOG = LogManager.getLogger(MatchingTreeBuilder.class);

   final CallTreeNode firstNode;
   final CallTreeNode secondNode;
   final Set<CallTreeNodeVertex> partition1 = new HashSet<>();
   final Set<CallTreeNodeVertex> partition2 = new HashSet<>();
   final Graph<CallTreeNodeVertex, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

   final Map<Integer, CallTreeNodeVertex> vertices1 = new HashMap<>();
   final Map<Integer, CallTreeNodeVertex> vertices2 = new HashMap<>();

   public MatchingTreeBuilder(final CallTreeNode firstNode, final CallTreeNode secondNode) {
      super();
      this.firstNode = firstNode;
      this.secondNode = secondNode;

      buildVertices(firstNode, partition1, vertices1);
      buildVertices(secondNode, partition2, vertices2);
   }

   private void buildVertices(final CallTreeNode firstNode, final Set<CallTreeNodeVertex> partition, final Map<Integer, CallTreeNodeVertex> vertices) {
      for (int firstChildIndex = 0; firstChildIndex < firstNode.getChildren().size(); firstChildIndex++) {
         final CallTreeNode child = firstNode.getChildren().get(firstChildIndex);
         final CallTreeNodeVertex vertex = new CallTreeNodeVertex(child);
         vertices.put(firstChildIndex, vertex);
         partition.add(vertex);
         graph.addVertex(vertex);
      }
   }

   public Set<CallTreeNodeVertex> getPartition1() {
      return partition1;
   }

   public Set<CallTreeNodeVertex> getPartition2() {
      return partition2;
   }

   public Graph<CallTreeNodeVertex, DefaultWeightedEdge> getGraph() {
      return graph;
   }

   public void buildEdges(final CallTreeNode firstNode, final CallTreeNode secondNode, final Graph<CallTreeNodeVertex, DefaultWeightedEdge> graph) {
      for (int firstChildIndex = 0; firstChildIndex < firstNode.getChildren().size(); firstChildIndex++) {
         for (int secondChildIndex = 0; secondChildIndex < secondNode.getChildren().size(); secondChildIndex++) {
            final CallTreeNode firstChild = firstNode.getChildren().get(firstChildIndex);
            final CallTreeNode secondChild = secondNode.getChildren().get(secondChildIndex);
            final DefaultWeightedEdge edge = graph.addEdge(
                  vertices1.get(firstChildIndex),
                  vertices2.get(secondChildIndex));
            final double weight = getWeight(firstChild, secondChild);
            LOG.trace("Edge: " + firstChild + " " + secondChild + " - " + weight);
            LOG.trace(edge);
            graph.setEdgeWeight(edge, weight);
         }
      }
   }

   private double getWeight(final CallTreeNode firstChild, final CallTreeNode secondChild) {
      double weight;
      if (firstChild.getKiekerPattern().equals(secondChild.getKiekerPattern())) {
         weight = 1000;
      } else if (firstChild.getCall().equals(secondChild.getCall())) {
         final int commonPrefixShare = getPrefixShare(firstChild, secondChild);
         weight = 300 + commonPrefixShare;
      } else {
         if (firstChild.getCall().equals("ADDED") || secondChild.getCall().equals("ADDED")) {
            weight = 1;
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
      }
      final int index1 = firstChild.getPosition();
      final int index2 = secondChild.getPosition();
      if (index1 == index2) {
         weight += 0.5;
      }
      if (firstChild.getChildren().size() == secondChild.getChildren().size()) {
         weight += 0.5;
      }
      return weight;
   }

   private static int getPrefixShare(final CallTreeNode firstChild, final CallTreeNode secondChild) {
      final String commonPrefix = StringUtils.getCommonPrefix(firstChild.getParameters(), secondChild.getParameters());
      final int averageLength = (firstChild.getParameters().length() + secondChild.getParameters().length()) / 2;
      final int commonPrefixShare = (int) (10d * commonPrefix.length() / averageLength);
      return commonPrefixShare;
   }
}
