package de.dagere.peass.dependencyprocessors;

import java.util.LinkedList;

/**
 * A comparator that guarantees deterministic order, but not by the original list
 * @author DaGeRe
 *
 */
public class CommitByNameComparator extends VersionComparatorInstance {
   
   public static final CommitByNameComparator INSTANCE = new CommitByNameComparator();
   
   private CommitByNameComparator() {
      super(new LinkedList<>());
   }

   @Override
   public int compare(String version1, String version2) {
      return version1.compareTo(version2);
   }

}
