package de.dagere.peass.dependencyprocessors;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import de.dagere.peass.dependency.persistence.SelectedTests;

public class CommitComparatorInstance implements Comparator<String> {
   
   public static final String NO_BEFORE = "NO_BEFORE";
   
   private final List<String> versions;

   public CommitComparatorInstance(List<String> versions) {
      this.versions = versions;
   }
   
   public CommitComparatorInstance(final SelectedTests dependencies2) {
      versions = new LinkedList<>();
      Arrays.stream(dependencies2.getVersionNames()).forEach(version -> versions.add(version));
   }

   @Override
   public int compare(String version1, String version2) {
      final int indexOf = versions.indexOf(version1);
      final int indexOf2 = versions.indexOf(version2);
      return indexOf - indexOf2;
   }
   
   public boolean isBefore(final String version, final String version2) {
      final int indexOf = versions.indexOf(version);
      final int indexOf2 = versions.indexOf(version2);
      return indexOf < indexOf2;
   }

   public List<String> getCommits() {
      return versions;
   }

   public int getVersionIndex(String version) {
      return versions.indexOf(version);
   }

   public String getPreviousVersion(final String version) {
      final int index = versions.indexOf(version);
      return index > 0 ? versions.get(index - 1) : NO_BEFORE;
   }
}
