package de.dagere.peass.dependencyprocessors;

import java.util.LinkedList;

/**
 * A comparator that guarantees deterministic order, but not by the original list
 * @author DaGeRe
 *
 */
public class CommitByNameComparator extends CommitComparatorInstance {
   
   public static final CommitByNameComparator INSTANCE = new CommitByNameComparator();
   
   private CommitByNameComparator() {
      super(new LinkedList<>());
   }

   @Override
   public int compare(String commit1, String commit2) {
      return commit1.compareTo(commit2);
   }

}
