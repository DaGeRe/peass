package de.dagere.peass.analysis.properties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.dagere.peass.dependencyprocessors.VersionComparator;

public class VersionChangeProperties {

   private Map<String, ChangeProperties> commits = VersionComparator.hasVersions() ? new TreeMap<>(VersionComparator.INSTANCE) : new LinkedHashMap<>();

   public Map<String, ChangeProperties> getVersions() {
      return commits;
   }

   public void setVersions(final Map<String, ChangeProperties> versionProperties) {
      this.commits = versionProperties;
   }

   public void executeProcessor(final PropertyProcessor c) {
      for (final Entry<String, ChangeProperties> commit : commits.entrySet()) {
         for (final Entry<String, List<ChangeProperty>> testcase : commit.getValue().getProperties().entrySet()) {
            for (final ChangeProperty change : testcase.getValue()) {
               c.process(commit.getKey(), testcase.getKey(), change, commit.getValue());
            }
         }
      }
   }
   
   final class Counter implements PropertyProcessor {
      int count = 0;
      @Override
      public void process(final String commit, final String testcase, final ChangeProperty change, final ChangeProperties changeProperties) {
         if (change.isAffectsSource() && !change.isAffectsTestSource()) {
            count++;
         }
      }
   };
   
   @JsonIgnore
   public int getSourceChanges() {
      final Counter counter = new Counter();
      executeProcessor(counter);
      return counter.count;
   }
}
