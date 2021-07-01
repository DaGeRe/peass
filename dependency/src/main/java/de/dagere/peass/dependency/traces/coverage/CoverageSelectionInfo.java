package de.dagere.peass.dependency.traces.coverage;

import java.util.LinkedHashMap;
import java.util.Map;

public class CoverageSelectionInfo {
   private Map<String, CoverageSelectionVersion> versions = new LinkedHashMap<>();

   public Map<String, CoverageSelectionVersion> getVersions() {
      return versions;
   }

   public void setVersions(final Map<String, CoverageSelectionVersion> versions) {
      this.versions = versions;
   }
}
