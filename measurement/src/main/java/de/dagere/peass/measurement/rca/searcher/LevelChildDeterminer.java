package de.dagere.peass.measurement.rca.searcher;

import java.util.LinkedList;
import java.util.List;

import de.dagere.peass.measurement.rca.data.CallTreeNode;

public class LevelChildDeterminer {
   public List<CallTreeNode> initialNodes;
   public List<CallTreeNode> selectedNodes = new LinkedList<>();
   
   public LevelChildDeterminer(final List<CallTreeNode> initialNodes, final int levels) {
      this.initialNodes = initialNodes;
      selectedNodes.addAll(initialNodes);
      List<CallTreeNode> iterationNodes = new LinkedList<>(initialNodes);
      for (int level = 0; level < levels; level++) {
         List<CallTreeNode> childs = new LinkedList<>();
         for (CallTreeNode parent : iterationNodes) {
            childs.addAll(parent.getChildren());
         }
         selectedNodes.addAll(childs);
         iterationNodes = childs;
      }
   }
   
   public List<CallTreeNode> getSelectedIncludingParentNodes() {
      return selectedNodes;
   }
   
   public List<CallTreeNode> getOnlyChildNodes(){
      List<CallTreeNode> childNodes = new LinkedList<>(selectedNodes);
      childNodes.removeAll(initialNodes);
      return childNodes;
   }
}
