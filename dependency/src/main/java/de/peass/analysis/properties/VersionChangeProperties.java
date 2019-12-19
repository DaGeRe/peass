package de.peass.analysis.properties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.peass.dependencyprocessors.VersionComparator;

public class VersionChangeProperties {

   private Map<String, ChangeProperties> versions = VersionComparator.hasDependencies() ? new TreeMap<>(VersionComparator.INSTANCE) : new LinkedHashMap<>();

   public Map<String, ChangeProperties> getVersions() {
      return versions;
   }

   public void setVersions(final Map<String, ChangeProperties> versionProperties) {
      this.versions = versionProperties;
   }

   public void executeProcessor(final PropertyProcessor c) {
      for (final Entry<String, ChangeProperties> version : versions.entrySet()) {
         for (final Entry<String, List<ChangeProperty>> testcase : version.getValue().getProperties().entrySet()) {
            for (final ChangeProperty change : testcase.getValue()) {
               c.process(version.getKey(), testcase.getKey(), change, version.getValue());
            }
         }
      }
   }
   
   final class Counter implements PropertyProcessor {
      int count = 0;
      @Override
      public void process(final String version, final String testcase, final ChangeProperty change, final ChangeProperties changeProperties) {
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
