package de.dagere.peass.visualization;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.dagere.peass.measurement.rca.data.BasicNode;
import de.dagere.peass.measurement.rca.treeanalysis.TreeUtil;

class EqualChildDeterminer {
   Map<String, List<BasicNode>> alreadyAdded = new HashMap<>();

   public boolean hasEqualSubtreeNode(final BasicNode current) {
      final List<BasicNode> candidates = alreadyAdded.get(current.getKiekerPattern());
      boolean foundEqual = false;
      if (candidates != null) {
         for (final BasicNode candidate : candidates) {
            if (TreeUtil.areTracesEqual(candidate, current)) {
               foundEqual = true;
            }
         }
         if (!foundEqual) {
            candidates.add(current);
         }
      } else {
         final LinkedList<BasicNode> list = new LinkedList<BasicNode>();
         alreadyAdded.put(current.getKiekerPattern(), list);
         list.add(current);
      }

      return foundEqual;
   }
}