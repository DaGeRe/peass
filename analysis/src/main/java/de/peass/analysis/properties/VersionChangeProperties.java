package de.peass.analysis.properties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.peass.dependencyprocessors.VersionComparator;
import de.peran.measurement.analysis.changes.processors.PropertyProcessor;

public class VersionChangeProperties {

   private Map<String, ChangeProperties> versions = VersionComparator.hasDependencies() ? new TreeMap<>(VersionComparator.INSTANCE) : new LinkedHashMap<>();

   public Map<String, ChangeProperties> getVersions() {
      return versions;
   }

   public void setVersions(Map<String, ChangeProperties> versionProperties) {
      this.versions = versionProperties;
   }

   public void executeProcessor(PropertyProcessor c) {
      for (Entry<String, ChangeProperties> version : versions.entrySet()) {
         for (Entry<String, List<ChangeProperty>> testcase : version.getValue().getProperties().entrySet()) {
            for (ChangeProperty change : testcase.getValue()) {
               c.process(version.getKey(), testcase.getKey(), change, version.getValue());
            }
         }
      }
   }
   
   final class Counter implements PropertyProcessor {
      int count = 0;
      @Override
      public void process(String version, String testcase, ChangeProperty change, ChangeProperties changeProperties) {
         if (change.isAffectsSource() && !change.isAffectsTestSource()) {
            count++;
         }
      }
   };
   
   @JsonIgnore
   public int getSourceChanges() {
      Counter counter = new Counter();
      executeProcessor(counter);
      return counter.count;
   }
}
