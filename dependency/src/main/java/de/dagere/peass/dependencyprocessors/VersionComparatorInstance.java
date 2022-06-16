package de.dagere.peass.dependencyprocessors;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import de.dagere.peass.dependency.persistence.StaticTestSelection;

public class VersionComparatorInstance implements Comparator<String> {
   private final List<String> versions;

   public VersionComparatorInstance(List<String> versions) {
      this.versions = versions;
   }
   
   public VersionComparatorInstance(final StaticTestSelection dependencies2) {
      versions = new LinkedList<>();
      versions.add(dependencies2.getInitialversion().getVersion());
      dependencies2.getVersions().keySet().stream().forEach(version -> versions.add(version));
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
}
