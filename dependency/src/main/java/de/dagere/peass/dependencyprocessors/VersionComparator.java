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

   private static List<String> versions;

   public static void setDependencies(final StaticTestSelection dependencies2) {
      versions = new LinkedList<>();
      versions.add(dependencies2.getInitialcommit().getCommit());
      dependencies2.getCommits().keySet().stream().forEach(version -> versions.add(version));
   }

   public static void setVersions(final List<String> commits) {
      versions = new LinkedList<>();
      commits.forEach(version -> versions.add(version));
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
