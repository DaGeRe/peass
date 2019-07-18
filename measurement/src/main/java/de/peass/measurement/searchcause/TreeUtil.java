package de.peass.measurement.searchcause;

import java.util.Iterator;

import de.peass.measurement.searchcause.data.CallTreeNode;

public class TreeUtil {
   public static boolean areTracesEqual(CallTreeNode firstNode, CallTreeNode secondNode) {
      if (!firstNode.getKiekerPattern().equals(secondNode.getKiekerPattern())) {
         return false;
      }
      if (firstNode.getChildren().size() != secondNode.getChildren().size()) {
         return false;
      }
      Iterator<CallTreeNode> firstIt = firstNode.getChildren().iterator();
      Iterator<CallTreeNode> secondIt = secondNode.getChildren().iterator();
      while (firstIt.hasNext() && secondIt.hasNext()) {
         CallTreeNode firstChild = firstIt.next();
         CallTreeNode secondChild = secondIt.next();
         if (!areTracesEqual(firstChild, secondChild)) {
            return false;
         }
      }

      return true;
   }

   public static boolean childrenEqual(CallTreeNode firstNode, CallTreeNode secondNode) {
      Iterator<CallTreeNode> firstIt = firstNode.getChildren().iterator();
      Iterator<CallTreeNode> secondIt = secondNode.getChildren().iterator();
      while (firstIt.hasNext() && secondIt.hasNext()) {
         CallTreeNode firstChild = firstIt.next();
         CallTreeNode secondChild = secondIt.next();
         if (!firstChild.getKiekerPattern().equals(secondChild.getKiekerPattern())) {
            return false;
         }
      }
      return true;
   }
}
