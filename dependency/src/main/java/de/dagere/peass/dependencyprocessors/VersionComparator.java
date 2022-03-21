package de.dagere.peass.dependencyprocessors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.dagere.peass.dependency.persistence.StaticTestSelection;
import de.dagere.peass.vcs.GitCommit;

/**
 * Compares versions regaring their index, i.e. if version a is seen as before version b in the commit log
 * 
 * @author reichelt
 *
 */
public class VersionComparator implements Comparator<String> {

   public static final String NO_BEFORE = "NO_BEFORE";

   private static final Logger LOG = LogManager.getLogger(VersionComparator.class);

   public static final VersionComparator INSTANCE = new VersionComparator();

   @Override
   public int compare(final String version1, final String version2) {
      final int indexOf = versions.indexOf(version1);
      final int indexOf2 = versions.indexOf(version2);
      return indexOf - indexOf2;
   }

   public static String getPreviousVersion(final String version) {
      final int index = versions.indexOf(version);
      return index > 0 ? versions.get(index - 1) : NO_BEFORE;
   }

   private static List<String> versions;

   public static void setDependencies(final StaticTestSelection dependencies2) {
      versions = new LinkedList<>();
      versions.add(dependencies2.getInitialversion().getVersion());
      dependencies2.getVersions().keySet().stream().forEach(version -> versions.add(version));
   }

   public static void setVersions(final List<GitCommit> commits) {
      versions = new LinkedList<>();
      commits.forEach(version -> versions.add(version.getTag()));
   }

   public static int getVersionIndex(final String version) {
      return versions.indexOf(version);
   }

   /**
    * Determines whether version is before version2
    * 
    * @param version
    * @param startversion
    * @return true, is version is before version2, false otherwise
    */
   public static boolean isBefore(final String version, final String version2) {
      final int indexOf = versions.indexOf(version);
      final int indexOf2 = versions.indexOf(version2);
      return indexOf < indexOf2;
   }

   public static boolean hasVersions() {
      return versions != null;
   }

   public Map<String, String> sort(final Map<String, String> commits) {
      final List<Map.Entry<String, String>> entries = new ArrayList<>(commits.entrySet());
      Collections.sort(entries, new Comparator<Map.Entry<String, String>>() {
         @Override
         public int compare(final Map.Entry<String, String> a, final Map.Entry<String, String> b) {
            return VersionComparator.this.compare(a.getKey(), b.getKey());
         }
      });
      final Map<String, String> sortedMap = new LinkedHashMap<>();
      for (final Map.Entry<String, String> entry : entries) {
         sortedMap.put(entry.getKey(), entry.getValue());
      }
      return sortedMap;
   }

}
