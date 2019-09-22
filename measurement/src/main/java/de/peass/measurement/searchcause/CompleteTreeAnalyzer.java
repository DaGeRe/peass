package de.peass.measurement.searchcause;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import de.peass.measurement.searchcause.data.CallTreeNode;

public class CompleteTreeAnalyzer {
   private final List<CallTreeNode> treeStructureDiffering = new LinkedList<>();
   private final List<CallTreeNode> nonDifferingVersion = new LinkedList<>();
   private final List<CallTreeNode> nonDifferingPredecessor = new LinkedList<>();

   public CompleteTreeAnalyzer(final CallTreeNode root, final CallTreeNode rootPredecessor) {
      getAllNodes(root, rootPredecessor);
   }

   private void getAllNodes(final CallTreeNode current, final CallTreeNode currentPredecessor) {
      if (current.getKiekerPattern().equals(currentPredecessor.getKiekerPattern()) &&
            currentPredecessor.getChildren().size() == current.getChildren().size()) {
         nonDifferingVersion.add(current);
         nonDifferingPredecessor.add(currentPredecessor);
         currentPredecessor.setOtherVersionNode(current);
         current.setOtherVersionNode(currentPredecessor);
         compareEqualChilds(current, currentPredecessor);
      } else {
         treeStructureDiffering.add(currentPredecessor);
      }
   }

   private void compareEqualChilds(final CallTreeNode current, final CallTreeNode currentPredecessor) {
      final Iterator<CallTreeNode> predecessorIterator = currentPredecessor.getChildren().iterator();
      final Iterator<CallTreeNode> currentIterator = current.getChildren().iterator();
      boolean oneHasNext = currentIterator.hasNext() && predecessorIterator.hasNext();
      while (oneHasNext) {
         final CallTreeNode currentPredecessorNode = predecessorIterator.next();
         final CallTreeNode currentVersionNode = currentIterator.next();
         getAllNodes(currentVersionNode, currentPredecessorNode);
         oneHasNext = currentIterator.hasNext() && predecessorIterator.hasNext();
      }
   }

   public List<CallTreeNode> getTreeStructureDiffering() {
      return treeStructureDiffering;
   }

   public List<CallTreeNode> getNonDifferingPredecessor() {
      return nonDifferingPredecessor;
   }

   public List<CallTreeNode> getNonDifferingVersion() {
      return nonDifferingVersion;
   }
}